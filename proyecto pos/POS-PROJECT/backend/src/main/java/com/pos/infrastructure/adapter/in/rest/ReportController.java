package com.pos.infrastructure.adapter.in.rest;

import com.pos.domain.model.InventoryReport;
import com.pos.domain.model.SalesReport;
import com.pos.domain.model.TopProductsReport;
import com.pos.domain.port.in.ReportGenerationPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportGenerationPort port;

    public ReportController(ReportGenerationPort port) {
        this.port = port;
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesReport> getSalesReport(@RequestParam String from, @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate toDate = LocalDate.parse(to, DateTimeFormatter.ISO_LOCAL_DATE);
        return ResponseEntity.ok(port.getSalesReport(fromDate, toDate));
    }

    @GetMapping("/top-products")
    public ResponseEntity<TopProductsReport> getTopProductsReport(@RequestParam String from, @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate toDate = LocalDate.parse(to, DateTimeFormatter.ISO_LOCAL_DATE);
        return ResponseEntity.ok(port.getTopProductsReport(fromDate, toDate));
    }

    @GetMapping("/inventory")
    public ResponseEntity<InventoryReport> getInventoryReport() {
        return ResponseEntity.ok(port.getInventoryReport());
    }
}
