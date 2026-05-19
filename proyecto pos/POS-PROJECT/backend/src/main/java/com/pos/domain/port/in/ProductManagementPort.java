package com.pos.domain.port.in;

import com.pos.domain.model.Product;
import java.math.BigDecimal;
import java.util.List;

public interface ProductManagementPort {
    Product createProduct(CreateProductCommand cmd);
    Product updateProduct(String id, UpdateProductCommand cmd);
    Product getProduct(String id);
    List<Product> getAllProducts();
    List<Product> searchProducts(String query);
    void deleteProduct(String id);

    record CreateProductCommand(String code, String name, BigDecimal price, int stockLevel, int lowStockThreshold) {}
    record UpdateProductCommand(String name, BigDecimal price, int stockLevel, int lowStockThreshold) {}
}
