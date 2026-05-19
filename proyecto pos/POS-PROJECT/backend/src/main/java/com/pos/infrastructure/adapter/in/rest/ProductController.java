package com.pos.infrastructure.adapter.in.rest;

import com.pos.domain.model.Product;
import com.pos.domain.port.in.ProductManagementPort;
import com.pos.infrastructure.adapter.in.rest.dto.CreateProductRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductManagementPort port;

    public ProductController(ProductManagementPort port) {
        this.port = port;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(port.getAllProducts());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String q) {
        return ResponseEntity.ok(port.searchProducts(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(port.getProduct(id));
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest req) {
        ProductManagementPort.CreateProductCommand cmd = new ProductManagementPort.CreateProductCommand(
                req.code(), req.name(), req.price(), req.stock_level(), req.low_stock_threshold()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(port.createProduct(cmd));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody CreateProductRequest req) {
        ProductManagementPort.UpdateProductCommand cmd = new ProductManagementPort.UpdateProductCommand(
                req.name(), req.price(), req.stock_level(), req.low_stock_threshold()
        );
        return ResponseEntity.ok(port.updateProduct(id, cmd));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        port.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
