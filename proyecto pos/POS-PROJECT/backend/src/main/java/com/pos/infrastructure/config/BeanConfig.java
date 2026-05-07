package com.pos.infrastructure.config;

import com.pos.application.usecase.CreateSaleUseCase;
import com.pos.application.usecase.GenerateReportUseCase;
import com.pos.application.usecase.ProcessPaymentUseCase;
import com.pos.application.usecase.ProductManagementUseCase;
import com.pos.domain.port.in.PaymentProcessingPort;
import com.pos.domain.port.in.ProductManagementPort;
import com.pos.domain.port.in.ReportGenerationPort;
import com.pos.domain.port.in.SaleManagementPort;
import com.pos.domain.port.out.PaymentRepository;
import com.pos.domain.port.out.ProductRepository;
import com.pos.domain.port.out.SaleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public ProductManagementPort productManagementPort(ProductRepository repo) {
        return new ProductManagementUseCase(repo);
    }

    @Bean
    public SaleManagementPort saleManagementPort(SaleRepository saleRepo, ProductRepository productRepo) {
        return new CreateSaleUseCase(saleRepo, productRepo);
    }

    @Bean
    public PaymentProcessingPort paymentProcessingPort(SaleRepository saleRepo, ProductRepository productRepo, PaymentRepository paymentRepo) {
        return new ProcessPaymentUseCase(saleRepo, productRepo, paymentRepo);
    }

    @Bean
    public ReportGenerationPort reportGenerationPort(SaleRepository saleRepo, PaymentRepository paymentRepo, ProductRepository productRepo) {
        return new GenerateReportUseCase(saleRepo, paymentRepo, productRepo);
    }
}
