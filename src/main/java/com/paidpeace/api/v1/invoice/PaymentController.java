package com.paidpeace.api.v1.invoice;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paidpeace.api.v1.invoice.dto.PaymentRequest;
import com.paidpeace.api.v1.invoice.dto.PaymentResponse;
import com.paidpeace.application.invoice.PaymentService;
import com.paidpeace.domain.invoice.payment.Payment;
import com.paidpeace.domain.invoice.payment.PaymentMode;

@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    // Create Payment for an Invoice
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@PathVariable UUID invoiceId, @RequestBody PaymentRequest paymentRequest) {
        Payment payment = paymentService.createPayment(invoiceId, paymentRequest);
        return ResponseEntity.ok(PaymentMapper.toResponse(payment));
    }

    // Get Payment by ID
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID invoiceId, @PathVariable UUID paymentId) {
        Payment payment = paymentService.getPayment(invoiceId, paymentId);
        return ResponseEntity.ok(PaymentMapper.toResponse(payment));
    }

    // Get All Payments for an Invoice
    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(
        @PathVariable UUID invoiceId,
        @RequestParam(required = false) PaymentMode mode,
        @RequestParam(required = false) Instant paidAt,
        Pageable pageable
    ) {
        Page<Payment> payments = paymentService.getPayments(invoiceId, mode, paidAt, pageable);
        return ResponseEntity.ok(payments.map(PaymentMapper::toResponse));
    }

    // Update Payment by ID
    @PatchMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> updatePayment(
        @PathVariable UUID invoiceId, 
        @PathVariable UUID paymentId, 
        @RequestBody PaymentRequest paymentRequest
    ) {
        Payment payment = paymentService.updatePayment(invoiceId, paymentId, paymentRequest);
        return ResponseEntity.ok(PaymentMapper.toResponse(payment));
    }
}
