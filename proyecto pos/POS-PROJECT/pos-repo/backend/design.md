# Design — Backend POS
## Java 17 + Spring Boot 3 | Hexagonal Architecture

---

## 1. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          BACKEND                                │
│                                                                 │
│  ┌──────────────────┐                                           │
│  │   infrastructure │  ← Adapters (REST controllers, JPA repos) │
│  │   (in / out)     │                                           │
│  └────────┬─────────┘                                           │
│           │ depends on                                          │
│  ┌────────▼─────────┐                                           │
│  │   application    │  ← Use Cases (business orchestration)     │
│  │   (use cases)    │                                           │
│  └────────┬─────────┘                                           │
│           │ depends on                                          │
│  ┌────────▼─────────┐                                           │
│  │     domain       │  ← Entities, Ports, Exceptions (pure Java)│
│  │  (model + ports) │                                           │
│  └──────────────────┘                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                          │
               ┌──────────▼──────────┐
               │     PostgreSQL       │
               └─────────────────────┘
```

**Golden rule:** Dependency arrows always point toward `domain`. The domain has no knowledge of Spring, JPA, or any framework.

---

## 2. Package Structure

```
backend/
└── src/
    ├── main/
    │   ├── java/com/pos/
    │   │   ├── domain/
    │   │   │   ├── model/
    │   │   │   │   ├── Product.java
    │   │   │   │   ├── Sale.java
    │   │   │   │   ├── SaleItem.java
    │   │   │   │   ├── Payment.java
    │   │   │   │   ├── SaleStatus.java          (enum)
    │   │   │   │   ├── PaymentMethod.java       (enum)
    │   │   │   │   ├── SalesReport.java
    │   │   │   │   ├── TopProductsReport.java
    │   │   │   │   └── InventoryReport.java
    │   │   │   ├── port/
    │   │   │   │   ├── in/
    │   │   │   │   │   ├── ProductManagementPort.java
    │   │   │   │   │   ├── SaleManagementPort.java
    │   │   │   │   │   ├── PaymentProcessingPort.java
    │   │   │   │   │   └── ReportGenerationPort.java
    │   │   │   │   └── out/
    │   │   │   │       ├── ProductRepository.java
    │   │   │   │       ├── SaleRepository.java
    │   │   │   │       └── PaymentRepository.java
    │   │   │   └── exception/
    │   │   │       ├── DuplicateProductException.java
    │   │   │       ├── InsufficientStockException.java
    │   │   │       ├── InvalidPaymentException.java
    │   │   │       ├── ProductHasSalesException.java
    │   │   │       └── SaleNotFoundException.java
    │   │   ├── application/
    │   │   │   └── usecase/
    │   │   │       ├── ProductManagementUseCase.java
    │   │   │       ├── CreateSaleUseCase.java
    │   │   │       ├── ProcessPaymentUseCase.java
    │   │   │       └── GenerateReportUseCase.java
    │   │   └── infrastructure/
    │   │       ├── adapter/
    │   │       │   ├── in/rest/
    │   │       │   │   ├── ProductController.java
    │   │       │   │   ├── SaleController.java
    │   │       │   │   ├── PaymentController.java
    │   │       │   │   ├── ReportController.java
    │   │       │   │   └── dto/
    │   │       │   │       ├── CreateProductRequest.java
    │   │       │   │       ├── AddItemRequest.java
    │   │       │   │       ├── ProcessPaymentRequest.java
    │   │       │   │       └── ErrorResponse.java
    │   │       │   └── out/persistence/
    │   │       │       ├── ProductRepositoryAdapter.java
    │   │       │       ├── SaleRepositoryAdapter.java
    │   │       │       ├── PaymentRepositoryAdapter.java
    │   │       │       ├── jpa/
    │   │       │       │   ├── ProductJpaRepository.java
    │   │       │       │   ├── SaleJpaRepository.java
    │   │       │       │   └── PaymentJpaRepository.java
    │   │       │       ├── entity/
    │   │       │       │   ├── ProductEntity.java
    │   │       │       │   ├── SaleEntity.java
    │   │       │       │   ├── SaleItemEntity.java
    │   │       │       │   └── PaymentEntity.java
    │   │       │       └── mapper/
    │   │       │           ├── ProductMapper.java
    │   │       │           ├── SaleMapper.java
    │   │       │           └── PaymentMapper.java
    │   │       ├── config/
    │   │       │   ├── BeanConfig.java
    │   │       │   └── OpenApiConfig.java
    │   │       └── exception/
    │   │           └── GlobalExceptionHandler.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/pos/
            ├── application/usecase/
            │   ├── ProductManagementUseCaseTest.java
            │   ├── CreateSaleUseCaseTest.java
            │   └── ProcessPaymentUseCaseTest.java
            └── infrastructure/adapter/in/rest/
                └── ProductControllerIntegrationTest.java
```

---

## 3. Domain Entities

```java
// Product.java
public class Product {
    private final String id;           // UUID
    private String code;               // unique in the system
    private String name;
    private BigDecimal price;          // invariant: > 0
    private int stockLevel;            // invariant: >= 0
    private int lowStockThreshold;     // default: 5
    private boolean outOfStock;

    public boolean isLowStock() {
        return stockLevel > 0 && stockLevel <= lowStockThreshold;
    }

    public void decrementStock(int quantity) {
        if (quantity > stockLevel) throw new InsufficientStockException(id, stockLevel, quantity);
        stockLevel -= quantity;
        outOfStock = (stockLevel == 0);
    }
}

// Sale.java
public class Sale {
    private final String id;
    private SaleStatus status;         // OPEN → CONFIRMED → PAID
    private final List<SaleItem> items = new ArrayList<>();
    private final LocalDateTime createdAt;

    public BigDecimal getTotal() {
        return items.stream()
            .map(SaleItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirm() {
        if (items.isEmpty()) throw new IllegalStateException("Sale has no items");
        this.status = SaleStatus.CONFIRMED;
    }

    public void markAsPaid() {
        this.status = SaleStatus.PAID;
    }
}

// SaleItem.java
public class SaleItem {
    private final String productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private int quantity;

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

// Payment.java
public class Payment {
    private final String id;
    private final String saleId;
    private final PaymentMethod method;
    private final BigDecimal amount;
    private final BigDecimal change;   // amount - sale.getTotal()
    private final LocalDateTime processedAt;
}
```

---

## 4. Inbound Ports (Driving Ports)

```java
// ProductManagementPort.java
public interface ProductManagementPort {
    Product createProduct(CreateProductCommand cmd);
    Product updateProduct(String id, UpdateProductCommand cmd);
    Product getProduct(String id);
    List<Product> getAllProducts();
    void deleteProduct(String id);
}

// SaleManagementPort.java
public interface SaleManagementPort {
    Sale createSale();
    Sale addItem(String saleId, AddItemCommand cmd);
    Sale confirmSale(String saleId);
    Sale getSale(String saleId);
}

// PaymentProcessingPort.java
public interface PaymentProcessingPort {
    Payment processPayment(String saleId, ProcessPaymentCommand cmd);
}

// ReportGenerationPort.java
public interface ReportGenerationPort {
    SalesReport getSalesReport(LocalDate from, LocalDate to);
    TopProductsReport getTopProductsReport(LocalDate from, LocalDate to);
    InventoryReport getInventoryReport();
}
```

---

## 5. Outbound Ports (Driven Ports)

```java
// ProductRepository.java
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(String id);
    Optional<Product> findByCode(String code);
    List<Product> findAll();
    void deleteById(String id);
    boolean hasSaleHistory(String productId);
}

// SaleRepository.java
public interface SaleRepository {
    Sale save(Sale sale);
    Optional<Sale> findById(String id);
    List<Sale> findByStatusAndPeriod(SaleStatus status, LocalDate from, LocalDate to);
}

// PaymentRepository.java
public interface PaymentRepository {
    Payment save(Payment payment);
    List<Payment> findByPeriod(LocalDate from, LocalDate to);
}
```

---

## 6. Use Cases (Application Layer)

```java
// ProcessPaymentUseCase.java — most complex use case example
@Transactional
public class ProcessPaymentUseCase implements PaymentProcessingPort {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    // Constructor injection — satisfies DIP
    public ProcessPaymentUseCase(
        SaleRepository saleRepository,
        ProductRepository productRepository,
        PaymentRepository paymentRepository
    ) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment processPayment(String saleId, ProcessPaymentCommand cmd) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException(saleId));

        if (sale.getStatus() != SaleStatus.CONFIRMED)
            throw new InvalidPaymentException("Sale is not CONFIRMED");

        if (cmd.amount().compareTo(sale.getTotal()) < 0)
            throw new InvalidPaymentException("Insufficient payment amount");

        // Decrement stock for each product
        for (SaleItem item : sale.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow();
            product.decrementStock(item.getQuantity());
            productRepository.save(product);
        }

        BigDecimal change = cmd.amount().subtract(sale.getTotal());
        Payment payment = new Payment(UUID.randomUUID().toString(), saleId,
            cmd.method(), cmd.amount(), change, LocalDateTime.now());

        paymentRepository.save(payment);
        sale.markAsPaid();
        saleRepository.save(sale);

        return payment;
    }
}
```

---

## 7. Bean Configuration (Spring DI)

```java
// BeanConfig.java — infrastructure/config
@Configuration
public class BeanConfig {

    @Bean
    public ProductManagementPort productManagementPort(ProductRepository repo) {
        return new ProductManagementUseCase(repo);
    }

    @Bean
    public SaleManagementPort saleManagementPort(
        SaleRepository saleRepo, ProductRepository productRepo
    ) {
        return new CreateSaleUseCase(saleRepo, productRepo);
    }

    @Bean
    public PaymentProcessingPort paymentProcessingPort(
        SaleRepository saleRepo,
        ProductRepository productRepo,
        PaymentRepository paymentRepo
    ) {
        return new ProcessPaymentUseCase(saleRepo, productRepo, paymentRepo);
    }

    @Bean
    public ReportGenerationPort reportGenerationPort(
        SaleRepository saleRepo, PaymentRepository paymentRepo, ProductRepository productRepo
    ) {
        return new GenerateReportUseCase(saleRepo, paymentRepo, productRepo);
    }
}
```

---

## 8. Error Handling

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateProductException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateProductException ex) {
        return new ErrorResponse("DUPLICATE_PRODUCT", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleStock(InsufficientStockException ex) {
        return new ErrorResponse("INSUFFICIENT_STOCK", ex.getMessage(), Instant.now());
    }

    // ... remaining exceptions
}

// ErrorResponse.java (DTO)
public record ErrorResponse(String error_code, String message, Instant timestamp) {}
```

| Exception                  | HTTP | error_code               |
|----------------------------|------|--------------------------|
| DuplicateProductException  | 409  | DUPLICATE_PRODUCT        |
| InsufficientStockException | 422  | INSUFFICIENT_STOCK       |
| InvalidPaymentException    | 422  | INVALID_PAYMENT          |
| ProductHasSalesException   | 422  | PRODUCT_HAS_SALES        |
| SaleNotFoundException      | 404  | SALE_NOT_FOUND           |
| IllegalArgumentException   | 400  | INVALID_REQUEST          |

---

## 9. API Contract — Endpoints

| Method | Path                           | Description                  | HTTP OK |
|--------|--------------------------------|------------------------------|---------|
| GET    | /api/v1/products               | List products                | 200     |
| GET    | /api/v1/products/{id}          | Get product by ID            | 200     |
| POST   | /api/v1/products               | Create product               | 201     |
| PUT    | /api/v1/products/{id}          | Update product               | 200     |
| DELETE | /api/v1/products/{id}          | Delete product               | 204     |
| POST   | /api/v1/sales                  | Create sale                  | 201     |
| GET    | /api/v1/sales/{id}             | Get sale by ID               | 200     |
| POST   | /api/v1/sales/{id}/items       | Add item to sale             | 200     |
| POST   | /api/v1/sales/{id}/confirm     | Confirm sale                 | 200     |
| POST   | /api/v1/payments               | Process payment              | 201     |
| GET    | /api/v1/reports/sales          | Sales report                 | 200     |
| GET    | /api/v1/reports/top-products   | Top 10 products              | 200     |
| GET    | /api/v1/reports/inventory      | Inventory report             | 200     |
| GET    | /api/v1/docs                   | OpenAPI 3.0 spec             | 200     |

---

## 10. JSON Models

```json
// POST /api/v1/products
{ "code": "PROD-001", "name": "Americano Coffee", "price": 2.50, "stock_level": 100, "low_stock_threshold": 10 }

// POST /api/v1/sales/{id}/items
{ "product_id": "uuid", "quantity": 2 }

// POST /api/v1/payments
{ "sale_id": "uuid", "method": "CASH", "amount": 10.00 }

// Response POST /api/v1/payments (201)
{ "id": "uuid", "sale_id": "uuid", "method": "CASH", "amount": 10.00, "change": 5.00, "processed_at": "2024-01-15T10:30:00Z" }

// Error response (all endpoints)
{ "error_code": "INSUFFICIENT_STOCK", "message": "Insufficient stock. Available: 3, requested: 5.", "timestamp": "2024-01-15T10:30:00Z" }
```
