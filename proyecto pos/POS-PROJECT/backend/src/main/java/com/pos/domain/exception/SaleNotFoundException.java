package com.pos.domain.exception;

public class SaleNotFoundException extends RuntimeException {
    public SaleNotFoundException(String id) {
        super("Sale not found with id: " + id);
    }
}
