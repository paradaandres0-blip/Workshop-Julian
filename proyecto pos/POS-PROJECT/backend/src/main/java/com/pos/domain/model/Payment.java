package com.pos.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private String id;
    private String saleId;
    private PaymentMethod method;
    private BigDecimal amount;
    private BigDecimal change;
    private LocalDateTime processedAt;
}
