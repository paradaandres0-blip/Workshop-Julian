package com.pos.domain.exception;

public class ProductHasSalesException extends RuntimeException {
    public ProductHasSalesException(String message) {
        super(message);
    }
}
