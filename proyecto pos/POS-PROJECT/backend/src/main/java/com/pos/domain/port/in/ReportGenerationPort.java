package com.pos.domain.port.in;

import com.pos.domain.model.InventoryReport;
import com.pos.domain.model.SalesReport;
import com.pos.domain.model.TopProductsReport;
import java.time.LocalDate;

public interface ReportGenerationPort {
    SalesReport getSalesReport(LocalDate from, LocalDate to);
    TopProductsReport getTopProductsReport(LocalDate from, LocalDate to);
    InventoryReport getInventoryReport();
}
