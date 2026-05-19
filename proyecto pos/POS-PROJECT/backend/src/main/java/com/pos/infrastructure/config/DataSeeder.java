package com.pos.infrastructure.config;

import com.pos.domain.model.*;
import com.pos.domain.port.out.PaymentRepository;
import com.pos.domain.port.out.ProductRepository;
import com.pos.domain.port.out.SaleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class DataSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;

    public DataSeeder(ProductRepository productRepository,
                      SaleRepository saleRepository,
                      PaymentRepository paymentRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedProducts();
        seedSalesAndPayments();
    }

    // ── Productos ────────────────────────────────────────────────────────────

    private void seedProducts() {
        List<Product> products = List.of(
            product("PROD-001", "Arroz Diana 500g",        new BigDecimal("2.50"),  120, 10),
            product("PROD-002", "Aceite Girasol 1L",       new BigDecimal("3.80"),   85, 10),
            product("PROD-003", "Leche Entera 1L",         new BigDecimal("1.20"),  200, 20),
            product("PROD-004", "Pan Tajado Bimbo",        new BigDecimal("2.10"),   60, 10),
            product("PROD-005", "Huevos x12",              new BigDecimal("4.50"),   45, 10),
            product("PROD-006", "Café Colcafé 250g",       new BigDecimal("5.90"),   30,  5),
            product("PROD-007", "Azúcar Blanca 1kg",       new BigDecimal("1.80"),  150, 15),
            product("PROD-008", "Sal Refisal 500g",        new BigDecimal("0.90"),  180, 20),
            product("PROD-009", "Pasta Doria 500g",        new BigDecimal("1.50"),   95, 10),
            product("PROD-010", "Atún Van Camps 170g",     new BigDecimal("2.20"),   70, 10),
            product("PROD-011", "Jabón Rey 300g",          new BigDecimal("1.10"),   55,  8),
            product("PROD-012", "Shampoo Head&Shoulders",  new BigDecimal("6.50"),   20,  5),
            product("PROD-013", "Papel Higiénico x4",      new BigDecimal("3.20"),   40,  8),
            product("PROD-014", "Detergente Ariel 500g",   new BigDecimal("4.10"),   35,  5),
            product("PROD-015", "Gaseosa Coca-Cola 2L",    new BigDecimal("2.80"),   80, 10),
            product("PROD-016", "Agua Cristal 600ml",      new BigDecimal("0.80"),  250, 30),
            product("PROD-017", "Jugo Hit Naranja 1L",     new BigDecimal("2.30"),   50, 10),
            product("PROD-018", "Galletas Oreo 176g",      new BigDecimal("1.90"),   65, 10),
            product("PROD-019", "Chocolate Jet 150g",      new BigDecimal("2.60"),   40,  8),
            product("PROD-020", "Mantequilla Alpina 125g", new BigDecimal("3.40"),   25,  5),
            // Productos con stock bajo
            product("PROD-021", "Queso Campesino 250g",    new BigDecimal("4.80"),    4,  5),
            product("PROD-022", "Yogur Alpina 200g",       new BigDecimal("1.60"),    3,  5),
            // Producto sin stock
            product("PROD-023", "Crema de Leche 200ml",    new BigDecimal("2.10"),    0,  5)
        );

        products.forEach(productRepository::save);
    }

    private Product product(String code, String name, BigDecimal price, int stock, int threshold) {
        return Product.builder()
                .id(UUID.randomUUID().toString())
                .code(code)
                .name(name)
                .price(price)
                .stockLevel(stock)
                .lowStockThreshold(threshold)
                .outOfStock(stock == 0)
                .build();
    }

    // ── Ventas y Pagos ───────────────────────────────────────────────────────

    private void seedSalesAndPayments() {
        // Buscar productos por código para usarlos en las ventas
        Product arroz    = findByCode("PROD-001");
        Product aceite   = findByCode("PROD-002");
        Product leche    = findByCode("PROD-003");
        Product pan      = findByCode("PROD-004");
        Product huevos   = findByCode("PROD-005");
        Product cafe     = findByCode("PROD-006");
        Product gaseosa  = findByCode("PROD-015");
        Product agua     = findByCode("PROD-016");
        Product galletas = findByCode("PROD-018");
        Product pasta    = findByCode("PROD-009");

        // Venta 1 — hace 5 días, pagada en efectivo
        createPaidSale(
                LocalDateTime.now().minusDays(5),
                PaymentMethod.CASH,
                new BigDecimal("20.00"),
                List.of(
                    item(arroz,   2),
                    item(leche,   3),
                    item(pan,     1)
                )
        );

        // Venta 2 — hace 4 días, tarjeta de crédito
        createPaidSale(
                LocalDateTime.now().minusDays(4),
                PaymentMethod.CREDIT_CARD,
                new BigDecimal("30.00"),
                List.of(
                    item(huevos,  1),
                    item(cafe,    1),
                    item(aceite,  1),
                    item(galletas,2)
                )
        );

        // Venta 3 — hace 3 días, tarjeta débito
        createPaidSale(
                LocalDateTime.now().minusDays(3),
                PaymentMethod.DEBIT_CARD,
                new BigDecimal("15.00"),
                List.of(
                    item(agua,    4),
                    item(gaseosa, 2),
                    item(pasta,   1)
                )
        );

        // Venta 4 — hace 2 días, efectivo
        createPaidSale(
                LocalDateTime.now().minusDays(2),
                PaymentMethod.CASH,
                new BigDecimal("25.00"),
                List.of(
                    item(arroz,   3),
                    item(aceite,  1),
                    item(leche,   2),
                    item(huevos,  1)
                )
        );

        // Venta 5 — ayer, tarjeta crédito
        createPaidSale(
                LocalDateTime.now().minusDays(1),
                PaymentMethod.CREDIT_CARD,
                new BigDecimal("40.00"),
                List.of(
                    item(cafe,    2),
                    item(galletas,3),
                    item(pan,     2),
                    item(gaseosa, 1)
                )
        );

        // Venta 6 — hoy, efectivo
        createPaidSale(
                LocalDateTime.now().minusHours(2),
                PaymentMethod.CASH,
                new BigDecimal("10.00"),
                List.of(
                    item(agua,    5),
                    item(pasta,   2)
                )
        );
    }

    private void createPaidSale(LocalDateTime date, PaymentMethod method,
                                 BigDecimal amountPaid, List<SaleItem> items) {
        String saleId = UUID.randomUUID().toString();

        Sale sale = Sale.builder()
                .id(saleId)
                .status(SaleStatus.PAID)
                .items(items)
                .createdAt(date)
                .build();

        saleRepository.save(sale);

        BigDecimal total = sale.getTotal();
        BigDecimal change = amountPaid.subtract(total).max(BigDecimal.ZERO);

        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .saleId(saleId)
                .method(method)
                .amount(amountPaid)
                .change(change)
                .processedAt(date.plusMinutes(2))
                .build();

        paymentRepository.save(payment);
    }

    private SaleItem item(Product product, int quantity) {
        return SaleItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(quantity)
                .build();
    }

    private Product findByCode(String code) {
        return productRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Seed product not found: " + code));
    }
}
