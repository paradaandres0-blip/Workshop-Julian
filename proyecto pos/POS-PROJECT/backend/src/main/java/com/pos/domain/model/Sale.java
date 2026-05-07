package com.pos.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sale {
    private String id;
    private SaleStatus status;
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();
    private LocalDateTime createdAt;

    public BigDecimal getTotal() {
        return items.stream()
            .map(SaleItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirm() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Sale has no items");
        }
        this.status = SaleStatus.CONFIRMED;
    }

    public void markAsPaid() {
        this.status = SaleStatus.PAID;
    }
}
