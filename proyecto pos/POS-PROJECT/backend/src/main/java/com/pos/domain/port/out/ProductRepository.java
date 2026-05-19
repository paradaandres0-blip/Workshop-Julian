package com.pos.domain.port.out;

import com.pos.domain.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(String id);
    Optional<Product> findByCode(String code);
    Optional<Product> findByName(String name);
    List<Product> findAll();
    void deleteById(String id);
    boolean hasSaleHistory(String productId);
}
