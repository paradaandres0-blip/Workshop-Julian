package com.pos.infrastructure.adapter.in.rest;

import com.pos.domain.model.Sale;
import com.pos.domain.port.in.SaleManagementPort;
import com.pos.infrastructure.adapter.in.rest.dto.AddItemRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleManagementPort port;

    public SaleController(SaleManagementPort port) {
        this.port = port;
    }

    @PostMapping
    public ResponseEntity<Sale> createSale() {
        return ResponseEntity.status(HttpStatus.CREATED).body(port.createSale());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSale(@PathVariable String id) {
        return ResponseEntity.ok(port.getSale(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Sale> addItem(@PathVariable String id, @RequestBody AddItemRequest req) {
        SaleManagementPort.AddItemCommand cmd = new SaleManagementPort.AddItemCommand(req.product_id(), req.quantity());
        return ResponseEntity.ok(port.addItem(id, cmd));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Sale> confirmSale(@PathVariable String id) {
        return ResponseEntity.ok(port.confirmSale(id));
    }
}
