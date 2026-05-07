package com.pos.infrastructure.adapter.out.persistence;

import com.pos.domain.model.Sale;
import com.pos.domain.model.SaleStatus;
import com.pos.domain.port.out.SaleRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class SaleRepositoryAdapter implements SaleRepository {

    private final Map<String, Sale> sales = new ConcurrentHashMap<>();

    @Override
    public Sale save(Sale sale) {
        sales.put(sale.getId(), sale);
        return sale;
    }

    @Override
    public Optional<Sale> findById(String id) {
        return Optional.ofNullable(sales.get(id));
    }

    @Override
    public List<Sale> findByStatusAndPeriod(SaleStatus status, LocalDate from, LocalDate to) {
        return sales.values().stream()
                .filter(s -> s.getStatus() == status)
                .filter(s -> !s.getCreatedAt().toLocalDate().isBefore(from) && !s.getCreatedAt().toLocalDate().isAfter(to))
                .collect(Collectors.toList());
    }
}
