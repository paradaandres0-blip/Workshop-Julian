# Implementation Tasks — Frontend POS
## Node.js 20 + Express 4 | Layered Architecture | SOLID | SDD

---

## Tasks

- [x] 1. Project initial setup
  - [x] 1.1 Initialize project with `npm init`; install production dependencies: `express`, `nunjucks`, `dotenv`; install dev dependencies: `jest`, `sinon`
  - [x] 1.2 Create `.env` with variables: `BACKEND_URL=http://localhost:8080`, `PORT=3000`
  - [x] 1.3 Create directory structure: `src/domain/model`, `src/domain/service`, `src/application`, `src/infrastructure/api`, `src/infrastructure/routes`, `src/views/sale`, `src/views/inventory`, `src/views/reports`, `src/composition`, `test/application`, `test/infrastructure/api`
  - _Specification: FR-05_

- [x] 2. Domain — Models and Interfaces
  - [x] 2.1 Define models with JSDoc: `product.js` `{ id, code, name, price, stockLevel, lowStock, outOfStock }`, `sale.js` `{ id, status, items: SaleItem[], total }`, `payment.js` `{ id, saleId, method, amount, change, processedAt }`
  - [x] 2.2 Define `IApiClient.js`: base class with methods `get(path)`, `post(path, body)`, `put(path, body)`, `delete(path)` — all throw `Error('Not implemented')`
  - [x] 2.3 Define service interfaces: `IProductService.js` (getAll, getById, create, update, delete), `ISaleService.js` (createSale, addItem, confirmSale, getSale), `IPaymentService.js` (processPayment), `IReportService.js` (getSalesReport, getTopProducts, getInventoryReport)
  - _Specification: FR-05, NFR-01_

- [x] 3. Infrastructure — ApiClient and ApiError
  - [x] 3.1 Implement `ApiError.js`: class extending `Error` with fields `errorCode` and `message`
  - [x] 3.2 Implement `ApiClient.js` extending `IApiClient`: private method `_request(method, path, body)` using native Fetch API; if `!res.ok` extract `{error_code, message}` from JSON and throw `ApiError`; return `null` for 204 responses
  - _Specification: FR-04, PBT-01_

- [x] 4. Infrastructure — API Services
  - [x] 4.1 Implement `ProductApiService.js` extending `IProductService`: constructor receives `IApiClient`; implement `getAll()` → GET `/api/v1/products`, `getById(id)` → GET `/api/v1/products/{id}`, `create(data)` → POST, `update(id, data)` → PUT, `delete(id)` → DELETE
  - [x] 4.2 Implement `SaleApiService.js`: `createSale()` → POST `/api/v1/sales`, `addItem(saleId, cmd)` → POST `/api/v1/sales/{id}/items`, `confirmSale(saleId)` → POST `/api/v1/sales/{id}/confirm`, `getSale(saleId)` → GET `/api/v1/sales/{id}`
  - [x] 4.3 Implement `PaymentApiService.js`: `processPayment(cmd)` → POST `/api/v1/payments` with body `{sale_id, method, amount}`
  - [x] 4.4 Implement `ReportApiService.js`: `getSalesReport(from, to)` → GET `/api/v1/reports/sales?from=&to=`, `getTopProducts(from, to)` → GET `/api/v1/reports/top-products?from=&to=`, `getInventoryReport()` → GET `/api/v1/reports/inventory`
  - _Specification: FR-04, NFR-02_

- [x] 5. Application — Orchestrators
  - [x] 5.1 Implement `SaleOrchestrator.js`: constructor `(ISaleService, IPaymentService)`; methods: `startSale()`, `addProduct(saleId, productId, qty)`, `checkout(saleId, method, amount)` — confirms the sale then processes the payment
  - [x] 5.2 Implement `InventoryOrchestrator.js`: constructor `(IProductService)`; methods: `listProducts()`, `createProduct(data)`, `updateProduct(id, data)`, `deleteProduct(id)`
  - [x] 5.3 Implement `ReportOrchestrator.js`: constructor `(IReportService)`; methods: `getSalesReport(from, to)`, `getTopProducts(from, to)`, `getInventoryReport()`
  - _Specification: FR-01, FR-02, FR-03, NFR-01, NFR-03_

- [x] 6. Infrastructure — Express Routes
  - [x] 6.1 Implement `saleRoutes.js` as factory `(saleOrchestrator) => router`: `GET /` renders `sale/sale.njk`; `POST /start` starts Sale; `POST /:id/item` adds product; `POST /:id/pay` executes checkout; handle `ApiError` returning `{ error: err.message }`
  - [x] 6.2 Implement `inventoryRoutes.js` as factory `(inventoryOrchestrator) => router`: `GET /` renders `inventory/inventory.njk` with product list; `POST /` creates product; `PUT /:id` updates; `DELETE /:id` deletes
  - [x] 6.3 Implement `reportRoutes.js` as factory `(reportOrchestrator) => router`: `GET /` renders `reports/reports.njk` with data from all three reports based on query params
  - _Specification: FR-01, FR-02, FR-03_

- [x] 7. Nunjucks Views
  - [x] 7.1 Create `views/layout.njk`: base template with navigation (Sale / Inventory / Reports), `content` block, basic CSS styles
  - [x] 7.2 Create `views/sale/sale.njk`: product search field, Sale_Items table with editable quantity and subtotal, sale total, payment form (Payment_Method selector + amount field), calculated change display area, error message area; inline JS for real-time subtotal and total updates
  - [x] 7.3 Create `views/inventory/inventory.njk`: paginated product table with CSS classes `low-stock` (yellow) and `out-of-stock` (red) based on flags; create/edit modal form with HTML5 validation (price > 0, stock >= 0); delete button with `confirm()`
  - [x] 7.4 Create `views/reports/reports.njk`: date filter form; sales table with breakdown by Payment_Method; top 10 products table; inventory table with stock indicators
  - _Specification: FR-01, FR-02, FR-03_

- [x] 8. Composition and Entry Point
  - [x] 8.1 Implement `composition/container.js`: instantiate `ApiClient(process.env.BACKEND_URL)`, all 4 `*ApiService`, all 3 `*Orchestrator`; export the 3 orchestrators
  - [x] 8.2 Implement `app.js`: load `dotenv`, configure Express, configure Nunjucks (`views/`), import `container`, register routes (`/sale`, `/inventory`, `/reports`), start server on `process.env.PORT`
  - _Specification: FR-05, NFR-03_

- [x] 9. Unit Tests
  - [x] 9.1 `SaleOrchestrator.test.js`: use Sinon to create stubs for `ISaleService` and `IPaymentService`; verify that `checkout()` calls `confirmSale()` before `processPayment()`; verify that if `confirmSale()` throws an error, `processPayment()` is not called
  - [x] 9.2 `ApiClient.test.js`: mock global `fetch`; verify that a 4xx response throws `ApiError` with the correct `errorCode` and `message`; verify that a 204 response returns `null`; verify that a 200 response returns the parsed JSON
  - _Specification: PBT-01, PBT-04_
