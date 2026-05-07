package com.pos.domain.port.out;

import com.pos.domain.model.Sale;
import com.pos.domain.model.SaleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleRepository {
    Sale save(Sale sale);
    Optional<Sale> findById(String id);
    List<Sale> findByStatusAndPeriod(SaleStatus status, LocalDate from, LocalDate to);
}
