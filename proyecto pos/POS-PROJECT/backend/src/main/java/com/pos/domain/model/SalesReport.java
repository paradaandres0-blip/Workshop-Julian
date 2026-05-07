package com.pos.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReport {
    private int totalSales;
    private BigDecimal totalAmount;
    private Map<PaymentMethod, BigDecimal> breakdownByMethod;
}
