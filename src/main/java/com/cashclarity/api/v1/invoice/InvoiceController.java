package com.cashclarity.api.v1.invoice;

import com.cashclarity.api.v1.invoice.dto.InvoiceRequest;
import com.cashclarity.api.v1.invoice.dto.InvoiceResponse;
import com.cashclarity.api.v1.invoice.dto.InvoiceUpdateRequest;
import com.cashclarity.application.invoice.CreateInvoiceService;
import com.cashclarity.application.invoice.InvoiceQueryService;
import com.cashclarity.application.invoice.UpdateInvoiceService;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.invoice.LifeCycleStatus;
import com.cashclarity.domain.invoice.TimeStatus;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.cashclarity.application.invoice.DeleteInvoiceService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
 

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invoices")
public class InvoiceController {

    private final CreateInvoiceService createInvoiceService;
    private final InvoiceQueryService invoiceQueryService;
    private final UpdateInvoiceService updateInvoiceService;
    private final DeleteInvoiceService deleteInvoiceService;

    public InvoiceController(CreateInvoiceService createInvoiceService, InvoiceQueryService invoiceQueryService, UpdateInvoiceService updateInvoiceService, DeleteInvoiceService deleteInvoiceService) {
        this.createInvoiceService = createInvoiceService;
        this.invoiceQueryService = invoiceQueryService;
        this.updateInvoiceService = updateInvoiceService;
        this.deleteInvoiceService = deleteInvoiceService;
    }

    /**
     * Creates a draft invoice.
     * Organization context comes from request payload, not from URI.
     */
    @PostMapping
    public ResponseEntity<InvoiceResponse> createDraft(@PathVariable Long organizationId, @Valid @RequestBody InvoiceRequest request) {
        Invoice created = createInvoiceService.createInvoice(organizationId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(InvoiceMapper.toResponse(created));
    }

    /**
     * Gets an invoice by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceQueryService.getInvoice(id);
        return ResponseEntity.ok(InvoiceMapper.toResponse(invoice));
    }

    /**
     * Updates mutable fields of a draft invoice.
     * Only allowed when invoice is in DRAFT lifecycle status.
     */
    @PatchMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> updateDraftInvoice(
            @PathVariable Long organizationId,
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceUpdateRequest request
    ) {
        Invoice updated = updateInvoiceService.updateInvoice(organizationId, invoiceId, request);
        return ResponseEntity.ok(InvoiceMapper.toResponse(updated));
    }

    /**
     * Hard-deletes a draft invoice. Only invoices in DRAFT lifecycle status may be deleted.
     */
    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<Void> deleteDraftInvoice(
            @PathVariable Long organizationId,
            @PathVariable Long invoiceId
    ) {
        deleteInvoiceService.deleteInvoice(organizationId, invoiceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getPaginatedInvoices(
        @PathVariable Long organizationId,
        @RequestParam(required = false) TimeStatus timeStatus,
        @RequestParam(required = false) LifeCycleStatus lifeCycleStatus,
        @RequestParam(required = false) String invoiceNumber,
        @RequestParam(required = false) LocalDate createdAt,
        @RequestParam(required = false) LocalDate updatedAt,
        @RequestParam(required = false) LocalDate dueDate,
        Pageable pageable
    ){
        // Currently support simple filters: lifecycle status and invoiceNumber.
        Page<Invoice> invoices = invoiceQueryService.list(
            organizationId, 
            timeStatus,
            lifeCycleStatus,
            invoiceNumber,
            createdAt,
            updatedAt,
            dueDate,
            pageable
        );
        return ResponseEntity.ok(invoices.map(InvoiceMapper::toResponse));
    }
}
