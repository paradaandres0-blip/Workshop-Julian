package com.pos.domain.port.in;

import com.pos.domain.model.Payment;
import com.pos.domain.model.PaymentMethod;
import java.math.BigDecimal;

public interface PaymentProcessingPort {
    Payment processPayment(String saleId, ProcessPaymentCommand cmd);

    record ProcessPaymentCommand(PaymentMethod method, BigDecimal amount) {}
}
