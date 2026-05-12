package com.pos.infrastructure.adapter.in.rest;

import com.pos.domain.model.Payment;
import com.pos.domain.port.in.PaymentProcessingPort;
import com.pos.infrastructure.adapter.in.rest.dto.ProcessPaymentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentProcessingPort port;

    public PaymentController(PaymentProcessingPort port) {
        this.port = port;
    }

    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody ProcessPaymentRequest req) {
        PaymentProcessingPort.ProcessPaymentCommand cmd = new PaymentProcessingPort.ProcessPaymentCommand(req.method(), req.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(port.processPayment(req.sale_id(), cmd));
    }
}
