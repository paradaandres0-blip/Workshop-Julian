package com.pos.infrastructure.adapter.in.rest.dto;

import com.pos.domain.model.PaymentMethod;
import java.math.BigDecimal;

public record CreateProductRequest(String code, String name, BigDecimal price, int stock_level, int low_stock_threshold) {}
