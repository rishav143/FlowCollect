package com.flowcollect.api.v1.invoice;

import java.time.LocalDate;
import java.util.UUID;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.invoice.dto.PaymentRequest;
import com.flowcollect.api.v1.invoice.dto.PaymentResponse;
import com.flowcollect.application.invoice.PaymentService;
import com.flowcollect.domain.invoice.payment.Payment;
import com.flowcollect.domain.invoice.payment.PaymentMode;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invoices/{invoiceId}/payments")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Create Payment for an Invoice
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @PathVariable UUID organizationId,
        @PathVariable UUID invoiceId,
        @RequestBody PaymentRequest paymentRequest
    ) {
        Payment payment = paymentService.createPayment(organizationId, invoiceId, paymentRequest);
        return ResponseEntity.status(201).body(PaymentMapper.toResponse(payment));
    }

    // Get Payment by ID
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
        @PathVariable UUID organizationId,
        @PathVariable UUID invoiceId,
        @PathVariable UUID paymentId
    ) {
        Payment payment = paymentService.getPayment(organizationId, invoiceId, paymentId);
        return ResponseEntity.ok(PaymentMapper.toResponse(payment));
    }

    // Get All Payments for an Invoice
    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(
        @PathVariable UUID organizationId,
        @PathVariable UUID invoiceId,
        @RequestParam(required = false) PaymentMode mode,
        @RequestParam(required = false) LocalDate paidOn,
        Pageable pageable
    ) {
        Page<Payment> payments = paymentService.getPayments(organizationId, invoiceId, mode, paidOn, pageable);
        return ResponseEntity.ok(payments.map(PaymentMapper::toResponse));
    }

    // Update Payment by ID
    @PatchMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> updatePayment(
        @PathVariable UUID organizationId,
        @PathVariable UUID invoiceId,
        @PathVariable UUID paymentId,
        @RequestBody PaymentRequest paymentRequest
    ) {
        Payment payment = paymentService.updatePayment(organizationId, invoiceId, paymentId, paymentRequest);
        return ResponseEntity.ok(PaymentMapper.toResponse(payment));
    }

    // Delete Payment by ID
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(
        @PathVariable UUID organizationId,
        @PathVariable UUID invoiceId,
        @PathVariable UUID paymentId
    ) {
        paymentService.deletePayment(organizationId, invoiceId, paymentId);
        return ResponseEntity.noContent().build();
    }
}
