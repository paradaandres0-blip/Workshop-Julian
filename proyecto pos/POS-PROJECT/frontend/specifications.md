# Specifications — Frontend POS
## Node.js 20 + Express 4 | Layered Architecture | SOLID | SDD

---

## 1. Introduction

The POS System Frontend is a web application built with Node.js 20 and Express 4. It applies SOLID principles and dependency inversion by organizing code into three layers: `domain`, `application`, and `infrastructure`. It exclusively consumes the Backend REST API and presents the sale, inventory, and reports screens.

**Technology stack:**
- Node.js 20
- Express 4
- Nunjucks (template engine)
- Native Fetch API (HTTP client)
- dotenv
- Jest + Sinon (unit testing)

---

## 2. SDD Glossary

| Term | Definition |
|---|---|
| **IApiClient** | Domain interface that abstracts all HTTP calls to the Backend |
| **ApiClient** | Concrete implementation of `IApiClient` using the Fetch API |
| **IService** | Domain service interface defined with JSDoc, with no HTTP dependencies |
| **Orchestrator** | `application` class that coordinates UI flows by injecting IServices via constructor |
| **DI Root** | `composition/container.js` — the single point where dependencies are instantiated and wired |
| **ApiError** | Error class that encapsulates `error_code` and `message` from the Backend |
| **Sale** | Active sale transaction on the Cashier's screen |
| **Sale_Item** | Product line within a Sale visible in the UI |
| **Product** | Item shown in the sale search or inventory screen |
| **Payment_Method** | Payment method: `CASH`, `CREDIT_CARD`, `DEBIT_CARD` |

---

## 3. Functional Requirements

### FR-01: Sale Screen

**User Story:** As a Cashier, I want a clear and efficient sale interface to register products and complete transactions without errors.

#### Acceptance Criteria

1. THE Frontend SHALL present a sale screen with a Product search field by code or name.
2. WHEN the Cashier selects a Product, THE Frontend SHALL add it to the Sale_Items list with an initial quantity of 1.
3. WHEN the Cashier modifies the quantity of a Sale_Item, THE Frontend SHALL update the subtotal and total in real time without reloading the page.
4. THE Frontend SHALL display the updated Sale total after each modification.
5. WHEN the Cashier confirms the Sale, THE Frontend SHALL request the Payment_Method and received amount before sending the Payment to the Backend.
6. WHEN the Backend returns the calculated change, THE Frontend SHALL display it on screen to the Cashier.
7. IF the Backend returns an error, THE Frontend SHALL display the error message without closing the active Sale.
8. WHEN the Sale is successfully paid, THE Frontend SHALL clear the screen and allow starting a new Sale.

---

### FR-02: Inventory Screen

**User Story:** As a Manager, I want to manage the product inventory to keep the POS catalog up to date.

#### Acceptance Criteria

1. THE Frontend SHALL present a paginated Product table with: code, name, price, Stock_Level, and status.
2. WHEN the Manager accesses the inventory, THE Frontend SHALL visually highlight Products with `low_stock: true` (yellow) and `out_of_stock: true` (red).
3. WHEN the Manager creates or edits a Product, THE Frontend SHALL validate: price > 0 and Stock_Level ≥ 0 before sending to the Backend.
4. IF the Backend returns HTTP 409, THE Frontend SHALL display the message: "Product code already exists".
5. WHEN the Manager deletes a Product, THE Frontend SHALL request confirmation before sending the request to the Backend.

---

### FR-03: Reports Screen

**User Story:** As a Manager, I want to query sales and inventory reports to analyze business performance.

#### Acceptance Criteria

1. THE Frontend SHALL present date filters (from / to) for sales and top products reports.
2. WHEN the Manager requests a report, THE Frontend SHALL display the results in sorted tables.
3. THE Frontend SHALL display the inventory report with visual indicators for `low_stock` and `out_of_stock`.
4. IF the Backend returns HTTP 400 for an invalid date range, THE Frontend SHALL display the error message in the form.

---

### FR-04: API Contract Consumption

**User Story:** As a development team, we want the Frontend to consume the Backend API in a decoupled way to guarantee independence between layers.

#### Acceptance Criteria

1. THE Frontend SHALL consume exclusively endpoints under `/api/v1` without directly accessing the database.
2. THE Frontend SHALL centralize all HTTP calls in `ApiClient`, implementing the `IApiClient` interface.
3. WHEN the Frontend receives an error from the Backend, THE Frontend SHALL extract the `message` field from the JSON `{error_code, message, timestamp}` to display to the user.
4. THE Frontend SHALL throw `ApiError(errorCode, message)` for any unsuccessful HTTP response.

---

### FR-05: SOLID Architecture and Dependency Inversion

**User Story:** As a development team, we want the Frontend to apply SOLID principles and dependency inversion to guarantee maintainability and testability.

#### Acceptance Criteria

1. THE Frontend SHALL organize code into three layers: `domain`, `application`, `infrastructure`.
2. THE Frontend SHALL define JSDoc interfaces for all domain services with no HTTP implementation dependencies.
3. THE Frontend SHALL inject all dependencies via constructor, without instantiating dependencies inside application modules.
4. THE Frontend SHALL implement `ApiClient` as a concrete class replaceable by mocks in tests.
5. WHEN the Frontend starts, THE Frontend SHALL register all dependencies in `composition/container.js`.

---

## 4. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | Orchestrators shall only know interfaces (IService), never concrete implementations |
| NFR-02 | `ApiClient` shall be the only class that makes HTTP calls to the Backend |
| NFR-03 | All dependency instantiation shall occur exclusively in `container.js` |
| NFR-04 | Nunjucks views shall contain no business logic |
| NFR-05 | Backend errors shall always be presented to the user using the `message` field from the error JSON |

---

## 5. Correctness Properties (Property-Based Testing)

| ID | Property |
|---|---|
| PBT-01 | For every HTTP error from the Backend, `ApiClient` throws `ApiError` with the correct `errorCode` and `message` |
| PBT-02 | For every Sale with N Sale_Items, the total shown in the UI = Σ(quantity_i × price_i) |
| PBT-03 | For every successful Payment, the change shown = entered amount − Sale total |
| PBT-04 | `SaleOrchestrator` never calls `ApiClient` directly; it always uses `ISaleService` |
| PBT-05 | For every Product with `out_of_stock: true`, the UI shows a red indicator |
| PBT-06 | For every Product with `low_stock: true`, the UI shows a yellow indicator |
