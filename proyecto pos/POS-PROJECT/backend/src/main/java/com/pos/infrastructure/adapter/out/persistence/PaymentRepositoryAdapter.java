package com.pos.infrastructure.adapter.out.persistence;

import com.pos.domain.model.Payment;
import com.pos.domain.port.out.PaymentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        payments.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public List<Payment> findByPeriod(LocalDate from, LocalDate to) {
        return payments.values().stream()
                .filter(p -> !p.getProcessedAt().toLocalDate().isBefore(from) && !p.getProcessedAt().toLocalDate().isAfter(to))
                .collect(Collectors.toList());
    }
}
