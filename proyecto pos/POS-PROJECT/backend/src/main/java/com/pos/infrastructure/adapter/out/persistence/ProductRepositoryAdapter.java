package com.pos.infrastructure.adapter.out.persistence;

import com.pos.domain.model.Product;
import com.pos.domain.port.out.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        products.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Optional<Product> findByCode(String code) {
        return products.values().stream()
                .filter(p -> p.getCode().equals(code))
                .findFirst();
    }

    @Override
    public Optional<Product> findByName(String name) {
        return products.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    @Override
    public void deleteById(String id) {
        products.remove(id);
    }

    @Override
    public boolean hasSaleHistory(String productId) {
        // Simple implementation for in-memory: always return false or implement cross-check
        // In a real local scenario, we'd check the SaleRepositoryAdapter
        return false;
    }
}
