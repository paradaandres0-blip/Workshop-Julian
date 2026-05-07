# Implementation Tasks — Backend POS
## Java 17 + Spring Boot 3 | Hexagonal Architecture | SDD

---

## Tasks

- [ ] 1. Project initial setup
  - [ ] 1.1 Create Spring Boot 3 project with dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `springdoc-openapi-starter-webmvc-ui`, `lombok`, `mapstruct`, `mapstruct-processor`
  - [ ] 1.2 Configure `application.yml`: PostgreSQL datasource, JPA (`ddl-auto: validate`), OpenAPI (`/api/v1/docs`), context prefix `/api/v1`
  - [ ] 1.3 Create empty package structure: `domain/model`, `domain/port/in`, `domain/port/out`, `domain/exception`, `application/usecase`, `infrastructure/adapter/in/rest/dto`, `infrastructure/adapter/out/persistence/jpa`, `infrastructure/adapter/out/persistence/entity`, `infrastructure/adapter/out/persistence/mapper`, `infrastructure/config`, `infrastructure/exception`
  - _Specification: NFR-01, NFR-02_

- [ ] 2. Domain — Entities
  - [ ] 2.1 Implement `Product`: fields `id` (UUID), `code`, `name`, `price` (BigDecimal), `stockLevel`, `lowStockThreshold` (default 5), `outOfStock`; methods `isLowStock()`, `decrementStock(int qty)` with insufficient stock validation
  - [ ] 2.2 Implement enum `SaleStatus`: `OPEN`, `CONFIRMED`, `PAID`
  - [ ] 2.3 Implement `SaleItem`: fields `productId`, `productName`, `unitPrice`, `quantity`; method `getSubtotal()`
  - [ ] 2.4 Implement `Sale`: list of `SaleItem`, `status`, `createdAt`; methods `getTotal()`, `confirm()` (validates non-empty items), `markAsPaid()`
  - [ ] 2.5 Implement enum `PaymentMethod`: `CASH`, `CREDIT_CARD`, `DEBIT_CARD`
  - [ ] 2.6 Implement `Payment`: fields `id`, `saleId`, `method`, `amount`, `change`, `processedAt`
  - [ ] 2.7 Implement report classes: `SalesReport` (totalSales, totalAmount, breakdownByMethod), `TopProductsReport` (list of ProductSalesSummary), `InventoryReport` (list of Products with flags)
  - _Specification: FR-01, FR-02, FR-03, FR-04_

- [ ] 3. Domain — Ports and Exceptions
  - [ ] 3.1 Define inbound ports (pure Java interfaces, no Spring annotations): `ProductManagementPort`, `SaleManagementPort`, `PaymentProcessingPort`, `ReportGenerationPort`
  - [ ] 3.2 Define outbound ports: `ProductRepository`, `SaleRepository`, `PaymentRepository`
  - [ ] 3.3 Implement domain exceptions (extend `RuntimeException`): `DuplicateProductException`, `InsufficientStockException`, `InvalidPaymentException`, `ProductHasSalesException`, `SaleNotFoundException`
  - _Specification: NFR-01, NFR-03_

- [ ] 4. Application — Use Cases
  - [ ] 4.1 Implement `ProductManagementUseCase` implementing `ProductManagementPort`: create (check unique code with `ProductRepository.findByCode`), update, query, delete (check `hasSaleHistory` before deleting)
  - [ ] 4.2 Implement `CreateSaleUseCase` implementing `SaleManagementPort`: `createSale()` creates OPEN Sale; `addItem()` validates stock and adds SaleItem; `confirmSale()` calls `sale.confirm()`; `getSale()` finds by id
  - [ ] 4.3 Implement `ProcessPaymentUseCase` implementing `PaymentProcessingPort` with `@Transactional`: validate CONFIRMED Sale, validate sufficient amount, decrement stock for each Product, create Payment with change, mark Sale as PAID
  - [ ] 4.4 Implement `GenerateReportUseCase` implementing `ReportGenerationPort`: `getSalesReport()` aggregates PAID Sales by period; `getTopProductsReport()` sorts products by quantity sold; `getInventoryReport()` returns all Products with flags
  - _Specification: FR-01, FR-02, FR-03, FR-04, NFR-02, NFR-05_

- [ ] 5. Infrastructure — JPA Persistence
  - [ ] 5.1 Implement JPA entities with `@Entity`, `@Table`, `@Column` annotations: `ProductEntity`, `SaleEntity`, `SaleItemEntity` (embeddable or entity with FK), `PaymentEntity`
  - [ ] 5.2 Implement Spring Data JPA interfaces: `ProductJpaRepository` (with `findByCode`), `SaleJpaRepository` (with query by status and date range), `PaymentJpaRepository` (with query by date range)
  - [ ] 5.3 Implement MapStruct mappers: `ProductMapper`, `SaleMapper`, `PaymentMapper` — bidirectional conversion between domain entities and JPA entities
  - [ ] 5.4 Implement repository adapters implementing the Driven Ports: `ProductRepositoryAdapter`, `SaleRepositoryAdapter`, `PaymentRepositoryAdapter` — use JpaRepository + Mapper internally
  - _Specification: NFR-06, FR-01_

- [ ] 6. Infrastructure — REST Controllers
  - [ ] 6.1 Implement `ProductController` (`@RestController`, `@RequestMapping("/products")`): `GET /`, `GET /{id}`, `POST /` (201), `PUT /{id}`, `DELETE /{id}` (204) — inject `ProductManagementPort` via constructor
  - [ ] 6.2 Implement `SaleController`: `POST /sales` (201), `GET /sales/{id}`, `POST /sales/{id}/items`, `POST /sales/{id}/confirm` — inject `SaleManagementPort`
  - [ ] 6.3 Implement `PaymentController`: `POST /payments` (201) — inject `PaymentProcessingPort`
  - [ ] 6.4 Implement `ReportController`: `GET /reports/sales`, `GET /reports/top-products`, `GET /reports/inventory` — inject `ReportGenerationPort`
  - [ ] 6.5 Implement `GlobalExceptionHandler` (`@RestControllerAdvice`): map each domain exception to its HTTP status and `ErrorResponse { error_code, message, timestamp }`
  - _Specification: FR-05, NFR-04_

- [ ] 7. Infrastructure — Configuration
  - [ ] 7.1 Implement `BeanConfig` (`@Configuration`): register the 4 Use Cases as `@Bean` with constructor injection of their Driven Ports
  - [ ] 7.2 Implement `OpenApiConfig`: configure title "POS API", version "1.0.0", description, base server `/api/v1`
  - _Specification: FR-05, NFR-04_

- [ ] 8. Unit Tests — Use Cases
  - [ ] 8.1 `ProductManagementUseCaseTest`: verify that creating with a duplicate code throws `DuplicateProductException`; verify that deleting with sale history throws `ProductHasSalesException`; verify that creating a valid product returns a Product with UUID
  - [ ] 8.2 `CreateSaleUseCaseTest`: verify that adding an item with insufficient stock throws `InsufficientStockException`; verify that confirming an empty Sale throws an exception; verify that `getTotal()` = Σ subtotals
  - [ ] 8.3 `ProcessPaymentUseCaseTest`: verify that insufficient amount throws `InvalidPaymentException`; verify that each Product's stock decreases by exactly the sold quantity; verify that change = amount − total
  - _Specification: PBT-01 through PBT-07_

- [ ] 9. Integration Tests — REST Controllers
  - [ ] 9.1 `ProductControllerIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`): verify that POST with duplicate code returns HTTP 409 with `error_code: DUPLICATE_PRODUCT`; verify that GET returns product list with correct `low_stock` flag
  - [ ] 9.2 `PaymentControllerIntegrationTest`: verify that POST with insufficient amount returns HTTP 422 with `error_code: INVALID_PAYMENT`; verify that a successful Payment returns HTTP 201 with correct `change`
  - _Specification: FR-05_
