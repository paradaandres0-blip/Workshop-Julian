# Specifications — Backend POS
## Java 17 + Spring Boot 3 | Hexagonal Architecture | SOLID | SDD

---

## 1. Introduction

The POS System Backend is a REST API built with Java 17 and Spring Boot 3. It implements hexagonal architecture (Ports & Adapters), SOLID principles, and dependency inversion. It is the only layer that accesses the database and exposes an API contract consumed by the Frontend.

**Technology stack:**
- Java 17
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- MapStruct (domain ↔ infrastructure mapping)
- Springdoc OpenAPI 3.0
- JUnit 5 + Mockito

---

## 2. SDD Glossary

| Term | Definition |
|---|---|
| **Port** | Abstract interface in the `domain` layer that defines an inbound or outbound contract |
| **Adapter** | Concrete implementation of a Port located in `infrastructure` |
| **Use_Case** | Class in `application` that orchestrates business logic using Ports |
| **Domain_Entity** | Object with identity and business invariants, with no framework dependencies |
| **Driving Port** | Inbound port (in) — initiated by external actors (HTTP, CLI) |
| **Driven Port** | Outbound port (out) — initiated by the domain (DB, messaging) |
| **Repository** | Driven Port that abstracts persistence access |
| **Sale** | Commercial transaction with one or more products |
| **Sale_Item** | Detail line of a Sale: product, quantity, unit price |
| **Product** | Item with a unique code, name, price, and stock level |
| **Payment** | Charge associated with a confirmed Sale |
| **Payment_Method** | Payment method: `CASH`, `CREDIT_CARD`, `DEBIT_CARD` |
| **Stock_Level** | Available quantity of a Product |
| **Low_Stock_Threshold** | Minimum stock threshold that triggers an alert |

---

## 3. Functional Requirements

### FR-01: Product Management

**User Story:** As a Manager, I want to manage the product catalog and inventory to keep availability up to date in the POS.

#### Acceptance Criteria

1. THE Backend SHALL expose `ProductManagementPort` with operations: create, update, query, and delete Products.
2. WHEN the Manager creates a Product with code, name, price, and Stock_Level, THE Backend SHALL persist it and return its generated UUID.
3. IF the Product code already exists, THEN THE Backend SHALL return HTTP 409 with `error_code: DUPLICATE_PRODUCT`.
4. WHEN the Manager updates a Product, THE Backend SHALL return the Product with the current data.
5. WHEN the Manager queries the inventory, THE Backend SHALL return all Products with their current Stock_Level.
6. WHILE Stock_Level ≤ Low_Stock_Threshold, THE Backend SHALL include `low_stock: true` in the Product response.
7. IF the Manager deletes a Product with associated Sales, THEN THE Backend SHALL return HTTP 422 with `error_code: PRODUCT_HAS_SALES`.
8. THE Backend SHALL access Product persistence exclusively through `ProductRepository` (Driven Port).

---

### FR-02: Sale Process

**User Story:** As a Cashier, I want to register sales with multiple products to process transactions quickly and accurately.

#### Acceptance Criteria

1. WHEN the Cashier starts a Sale, THE Backend SHALL create it in `OPEN` state and return its UUID.
2. WHEN the Cashier adds a Sale_Item, THE Backend SHALL validate the Product exists and Stock_Level is sufficient.
3. IF the requested quantity exceeds the Stock_Level, THEN THE Backend SHALL return HTTP 422 with `error_code: INSUFFICIENT_STOCK` and the available stock.
4. WHEN a Sale_Item is added, THE Backend SHALL calculate subtotal = quantity × current unit price.
5. THE Backend SHALL calculate the Sale total as the sum of all Sale_Item subtotals.
6. WHEN the Cashier confirms the Sale, THE Backend SHALL change status to `CONFIRMED` and return the total.
7. IF the Cashier confirms a Sale with no Sale_Items, THEN THE Backend SHALL return HTTP 422 with `error_code: EMPTY_SALE`.
8. THE Backend SHALL implement this logic in `CreateSaleUseCase` operating exclusively over Ports.

---

### FR-03: Payment Processing

**User Story:** As a Cashier, I want to register payment for a sale with different methods to complete the transaction and update inventory.

#### Acceptance Criteria

1. WHEN the Cashier registers a Payment for a `CONFIRMED` Sale, THE Backend SHALL validate that the amount ≥ Sale total.
2. IF the amount is insufficient, THEN THE Backend SHALL return HTTP 422 with `error_code: INSUFFICIENT_PAYMENT` and the pending difference.
3. WHEN the Payment is successful, THE Backend SHALL: change Sale to `PAID`, decrement Stock_Level for each Product, and return the calculated change.
4. THE Backend SHALL accept Payment_Methods: `CASH`, `CREDIT_CARD`, `DEBIT_CARD`.
5. IF the Payment_Method is not valid, THEN THE Backend SHALL return HTTP 400 with `error_code: INVALID_PAYMENT_METHOD`.
6. WHEN a Product's Stock_Level reaches 0, THE Backend SHALL mark it as `out_of_stock: true`.
7. THE Backend SHALL implement this logic in `ProcessPaymentUseCase` with `@Transactional` guaranteeing atomicity.

---

### FR-04: Reports

**User Story:** As a Manager, I want to query sales reports by period to analyze business performance.

#### Acceptance Criteria

1. WHEN the Manager requests a sales report with start and end dates, THE Backend SHALL return: total `PAID` Sales, total amount collected, and breakdown by Payment_Method.
2. WHEN the Manager requests top products, THE Backend SHALL return the 10 Products with the highest quantity sold in the period, sorted descending.
3. WHEN the Manager requests an inventory report, THE Backend SHALL return all Products with Stock_Level, `low_stock`, and `out_of_stock`.
4. IF start date > end date, THEN THE Backend SHALL return HTTP 400 with `error_code: INVALID_DATE_RANGE`.
5. THE Backend SHALL implement this logic in `GenerateReportUseCase`.

---

### FR-05: REST API Contract

**User Story:** As a development team, we want a well-defined API contract to guarantee reliable integration with the Frontend.

#### Acceptance Criteria

1. THE Backend SHALL expose all endpoints under the `/api/v1` prefix.
2. THE Backend SHALL return all responses with `Content-Type: application/json`.
3. THE Backend SHALL document all endpoints in OpenAPI 3.0 accessible at `/api/v1/docs`.
4. WHEN the Backend receives malformed JSON, THE Backend SHALL return HTTP 400 with `error_code: MALFORMED_REQUEST`.
5. THE Backend SHALL include in all error responses: `{ "error_code": string, "message": string, "timestamp": ISO8601 }`.

---

## 4. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | No class in `domain` shall import classes from `application` or `infrastructure` |
| NFR-02 | No class in `application` shall import classes from `infrastructure` |
| NFR-03 | All Ports shall be pure Java interfaces with no Spring annotations |
| NFR-04 | All dependency injection shall be via constructor (Constructor Injection) |
| NFR-05 | All multi-repository operations shall be atomic with `@Transactional` |
| NFR-06 | Mappers between domain and infrastructure shall be explicit (MapStruct) |
| NFR-07 | Response time for sale endpoints shall be < 500ms under normal load |

---

## 5. Correctness Properties (Property-Based Testing)

| ID | Property |
|---|---|
| PBT-01 | For every Product created with price P > 0, `getPrice()` returns P |
| PBT-02 | For every Sale with N Sale_Items, `getTotal()` = Σ(quantity_i × price_i) |
| PBT-03 | For every Payment with amount M ≥ total T, the returned change = M − T |
| PBT-04 | For every successful Payment, each Product's Stock_Level decreases by exactly the sold quantity |
| PBT-05 | For every Product with Stock_Level = 0 after a Payment, `out_of_stock` = true |
| PBT-06 | A Sale with 0 Sale_Items can never be confirmed |
| PBT-07 | A Payment can never be processed on a Sale that is not in `CONFIRMED` state |
