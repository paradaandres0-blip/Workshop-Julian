package com.pos.infrastructure.exception;

import com.pos.domain.exception.*;
import com.pos.infrastructure.adapter.in.rest.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateProductException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateProductException(DuplicateProductException ex) {
        return new ErrorResponse("DUPLICATE_PRODUCT", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleInsufficientStockException(InsufficientStockException ex) {
        return new ErrorResponse("INSUFFICIENT_STOCK", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(InvalidPaymentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleInvalidPaymentException(InvalidPaymentException ex) {
        return new ErrorResponse("INVALID_PAYMENT", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(ProductHasSalesException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleProductHasSalesException(ProductHasSalesException ex) {
        return new ErrorResponse("PRODUCT_HAS_SALES", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(SaleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleSaleNotFoundException(SaleNotFoundException ex) {
        return new ErrorResponse("SALE_NOT_FOUND", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ErrorResponse("INVALID_REQUEST", ex.getMessage(), Instant.now());
    }
    
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleIllegalStateException(IllegalStateException ex) {
        return new ErrorResponse("INVALID_STATE", ex.getMessage(), Instant.now());
    }
}
