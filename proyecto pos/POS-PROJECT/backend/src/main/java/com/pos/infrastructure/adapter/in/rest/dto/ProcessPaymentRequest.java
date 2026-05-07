package com.pos.infrastructure.adapter.in.rest.dto;

import com.pos.domain.model.PaymentMethod;
import java.math.BigDecimal;

public record ProcessPaymentRequest(String sale_id, PaymentMethod method, BigDecimal amount) {}
