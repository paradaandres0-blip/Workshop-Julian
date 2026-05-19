package com.pos.application.usecase;

import com.pos.domain.exception.DuplicateProductException;
import com.pos.domain.exception.ProductHasSalesException;
import com.pos.domain.model.Product;
import com.pos.domain.port.in.ProductManagementPort;
import com.pos.domain.port.out.ProductRepository;

import java.util.List;
import java.util.UUID;

public class ProductManagementUseCase implements ProductManagementPort {

    private final ProductRepository productRepository;

    public ProductManagementUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product createProduct(CreateProductCommand cmd) {
        if (productRepository.findByCode(cmd.code()).isPresent()) {
            throw new DuplicateProductException("Product with code " + cmd.code() + " already exists.");
        }
        
        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .code(cmd.code())
                .name(cmd.name())
                .price(cmd.price())
                .stockLevel(cmd.stockLevel())
                .lowStockThreshold(cmd.lowStockThreshold())
                .outOfStock(cmd.stockLevel() == 0)
                .build();
                
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(String id, UpdateProductCommand cmd) {
        Product product = getProduct(id);
        
        product.setName(cmd.name());
        product.setPrice(cmd.price());
        product.setStockLevel(cmd.stockLevel());
        product.setLowStockThreshold(cmd.lowStockThreshold());
        product.setOutOfStock(cmd.stockLevel() == 0);
        
        return productRepository.save(product);
    }

    @Override
    public Product getProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> searchProducts(String query) {
        String q = query.toLowerCase().trim();
        return productRepository.findAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(q)
                          || p.getCode().toLowerCase().contains(q))
                .limit(10)
                .toList();
    }

    @Override
    public void deleteProduct(String id) {
        if (productRepository.hasSaleHistory(id)) {
            throw new ProductHasSalesException("Product has associated sales and cannot be deleted.");
        }
        productRepository.deleteById(id);
    }
}
