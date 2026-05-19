package com.pos.application.usecase;

import com.pos.domain.exception.SaleNotFoundException;
import com.pos.domain.model.Product;
import com.pos.domain.model.Sale;
import com.pos.domain.model.SaleItem;
import com.pos.domain.model.SaleStatus;
import com.pos.domain.port.in.SaleManagementPort;
import com.pos.domain.port.out.ProductRepository;
import com.pos.domain.port.out.SaleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

public class CreateSaleUseCase implements SaleManagementPort {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public CreateSaleUseCase(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Sale createSale() {
        Sale sale = Sale.builder()
                .id(UUID.randomUUID().toString())
                .status(SaleStatus.OPEN)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
                
        return saleRepository.save(sale);
    }

    @Override
    public Sale addItem(String saleId, AddItemCommand cmd) {
        Sale sale = getSale(saleId);
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw new IllegalStateException("Cannot add items to a sale that is not OPEN.");
        }
        
        Product product = productRepository.findById(cmd.productId())
                .or(() -> productRepository.findByCode(cmd.productId()))
                .or(() -> productRepository.findByName(cmd.productId()))
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
                
        if (product.getStockLevel() < cmd.quantity()) {
            throw new com.pos.domain.exception.InsufficientStockException(product.getId(), product.getStockLevel(), cmd.quantity());
        }
        
        SaleItem item = SaleItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(cmd.quantity())
                .build();
                
        sale.getItems().add(item);
        
        return saleRepository.save(sale);
    }

    @Override
    public Sale confirmSale(String saleId) {
        Sale sale = getSale(saleId);
        sale.confirm();
        return saleRepository.save(sale);
    }

    @Override
    public Sale getSale(String saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException(saleId));
    }
}
