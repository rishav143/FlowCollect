package com.cashclarity.api.v1.invoice;

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

import com.cashclarity.api.v1.invoice.dto.PaymentRequest;
import com.cashclarity.api.v1.invoice.dto.PaymentResponse;
import com.cashclarity.application.invoice.PaymentService;
import com.cashclarity.domain.invoice.payment.Payment;
import com.cashclarity.domain.invoice.payment.PaymentMode;

@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * Creates a new payment for an invoice.
     * @param invoiceId The ID of the invoice to create a payment for.
     * @param paymentRequest The request body containing the payment details.
     * @return The created payment.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@PathVariable UUID invoiceId, @RequestBody PaymentRequest paymentRequest) {
        Payment payment = paymentService.createPayment(invoiceId, paymentRequest);
        return ResponseEntity.ok(PaymentMapper.toResponse(payment));
    }

    /**
     * Gets a payment by its ID.
     * @param invoiceId The ID of the invoice to get a payment for.
     * @param paymentId The ID of the payment to get.
     * @return The payment.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID invoiceId, @PathVariable UUID paymentId) {
        Payment payment = paymentService.getPayment(invoiceId, paymentId);
        return ResponseEntity.ok(PaymentMapper.toResponse(payment));
    }

    /**
     * Gets all payments for an invoice.
     * @param invoiceId The ID of the invoice to get payments for.
     * @param mode The mode of the payments to get.
     * @param paidAt The paid at date of the payments to get.
     * @param pageable The pageable object to get the payments.
     * @return The payments.
     */
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

    /**
     * Updates a payment by its ID.
     * @param invoiceId The ID of the invoice to update the payment for.
     * @param paymentId The ID of the payment to update.
     * @param paymentRequest The request body containing the payment update request.
     * @return The updated payment.
     */
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
