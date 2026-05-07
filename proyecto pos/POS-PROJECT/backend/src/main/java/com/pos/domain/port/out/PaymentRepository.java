package com.pos.domain.port.out;

import com.pos.domain.model.Payment;
import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository {
    Payment save(Payment payment);
    List<Payment> findByPeriod(LocalDate from, LocalDate to);
}
