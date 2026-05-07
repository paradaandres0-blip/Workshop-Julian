package com.pos.domain.model;

import com.pos.domain.exception.InsufficientStockException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String code;
    private String name;
    private BigDecimal price;
    private int stockLevel;
    @Builder.Default
    private int lowStockThreshold = 5;
    private boolean outOfStock;

    public boolean isLowStock() {
        return stockLevel > 0 && stockLevel <= lowStockThreshold;
    }

    public void decrementStock(int quantity) {
        if (quantity > stockLevel) {
            throw new InsufficientStockException(id, stockLevel, quantity);
        }
        stockLevel -= quantity;
        outOfStock = (stockLevel == 0);
    }
}
