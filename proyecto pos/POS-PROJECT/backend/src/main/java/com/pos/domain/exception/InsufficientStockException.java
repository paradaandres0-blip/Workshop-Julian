package com.pos.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String id, int available, int requested) {
        super(String.format("Insufficient stock for product %s. Available: %d, requested: %d.", id, available, requested));
    }
}
