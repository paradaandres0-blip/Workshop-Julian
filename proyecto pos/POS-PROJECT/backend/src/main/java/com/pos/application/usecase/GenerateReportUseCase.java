package com.pos.application.usecase;

import com.pos.domain.model.InventoryReport;
import com.pos.domain.model.Payment;
import com.pos.domain.model.PaymentMethod;
import com.pos.domain.model.Product;
import com.pos.domain.model.ProductSalesSummary;
import com.pos.domain.model.Sale;
import com.pos.domain.model.SaleItem;
import com.pos.domain.model.SaleStatus;
import com.pos.domain.model.SalesReport;
import com.pos.domain.model.TopProductsReport;
import com.pos.domain.port.in.ReportGenerationPort;
import com.pos.domain.port.out.PaymentRepository;
import com.pos.domain.port.out.ProductRepository;
import com.pos.domain.port.out.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateReportUseCase implements ReportGenerationPort {

    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    public GenerateReportUseCase(SaleRepository saleRepository, PaymentRepository paymentRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
    }

    @Override
    public SalesReport getSalesReport(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("INVALID_DATE_RANGE");
        }
        
        List<Sale> paidSales = saleRepository.findByStatusAndPeriod(SaleStatus.PAID, from, to);
        List<Payment> payments = paymentRepository.findByPeriod(from, to);

        BigDecimal totalAmount = payments.stream()
                .map(p -> p.getAmount().subtract(p.getChange()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<PaymentMethod, BigDecimal> breakdown = new HashMap<>();
        for (Payment p : payments) {
            BigDecimal actualPayment = p.getAmount().subtract(p.getChange());
            breakdown.put(p.getMethod(), breakdown.getOrDefault(p.getMethod(), BigDecimal.ZERO).add(actualPayment));
        }

        return new SalesReport(paidSales.size(), totalAmount, breakdown);
    }

    @Override
    public TopProductsReport getTopProductsReport(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("INVALID_DATE_RANGE");
        }
        
        List<Sale> paidSales = saleRepository.findByStatusAndPeriod(SaleStatus.PAID, from, to);
        
        Map<String, ProductSalesSummary> summaryMap = new HashMap<>();
        
        for (Sale sale : paidSales) {
            for (SaleItem item : sale.getItems()) {
                summaryMap.compute(item.getProductId(), (id, existing) -> {
                    if (existing == null) {
                        return new ProductSalesSummary(id, item.getProductName(), item.getQuantity(), item.getSubtotal());
                    } else {
                        existing.setQuantitySold(existing.getQuantitySold() + item.getQuantity());
                        existing.setTotalRevenue(existing.getTotalRevenue().add(item.getSubtotal()));
                        return existing;
                    }
                });
            }
        }
        
        List<ProductSalesSummary> topProducts = summaryMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.getQuantitySold(), a.getQuantitySold()))
                .limit(10)
                .collect(Collectors.toList());
                
        return new TopProductsReport(topProducts);
    }

    @Override
    public InventoryReport getInventoryReport() {
        List<Product> products = productRepository.findAll();
        return new InventoryReport(products);
    }
}
