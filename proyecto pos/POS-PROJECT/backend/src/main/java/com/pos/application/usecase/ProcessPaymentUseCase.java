package com.pos.application.usecase;

import com.pos.domain.exception.InvalidPaymentException;
import com.pos.domain.exception.SaleNotFoundException;
import com.pos.domain.model.Payment;
import com.pos.domain.model.Product;
import com.pos.domain.model.Sale;
import com.pos.domain.model.SaleItem;
import com.pos.domain.model.SaleStatus;
import com.pos.domain.port.in.PaymentProcessingPort;
import com.pos.domain.port.out.PaymentRepository;
import com.pos.domain.port.out.ProductRepository;
import com.pos.domain.port.out.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessPaymentUseCase implements PaymentProcessingPort {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    public ProcessPaymentUseCase(SaleRepository saleRepository, ProductRepository productRepository, PaymentRepository paymentRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment processPayment(String saleId, ProcessPaymentCommand cmd) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException(saleId));

        if (sale.getStatus() != SaleStatus.CONFIRMED) {
            throw new InvalidPaymentException("Sale is not CONFIRMED");
        }

        if (cmd.amount().compareTo(sale.getTotal()) < 0) {
            throw new InvalidPaymentException("Insufficient payment amount");
        }

        for (SaleItem item : sale.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
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
