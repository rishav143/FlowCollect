package com.flowcollect.api.v1.invoice;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.flowcollect.api.v1.invoice.dto.InvoiceRequest;
import com.flowcollect.api.v1.invoice.dto.InvoiceResponse;
import com.flowcollect.api.v1.invoice.dto.InvoiceUpdateRequest;
import com.flowcollect.api.v1.invoice.dto.IssueInvoiceRequest;
import com.flowcollect.application.confirmation.ConfirmationLinkService;
import com.flowcollect.application.invoice.FollowUpService;
import com.flowcollect.application.invoice.InvoicePdfFile;
import com.flowcollect.application.invoice.InvoiceService;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.TimeStatus;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
 

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invoices")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final FollowUpService followUpService;
    private final ConfirmationLinkService confirmationLinkService;

    public InvoiceController(
            InvoiceService invoiceService,
            FollowUpService followUpService,
            ConfirmationLinkService confirmationLinkService
    ) {
        this.invoiceService = invoiceService;
        this.followUpService = followUpService;
        this.confirmationLinkService = confirmationLinkService;
    }

    /**
     * Creates a draft invoice.
     * Organization context comes from request payload, not from URI.
     */
    @PostMapping
    public ResponseEntity<InvoiceResponse> createDraft
    (
        @PathVariable UUID organizationId, 
        @Valid @RequestBody InvoiceRequest request
    ) {
        Invoice created = invoiceService.createInvoice(organizationId, request);
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
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoice
    (
        @PathVariable UUID organizationId,
        @PathVariable UUID invoiceId
    ) {
        Invoice invoice = invoiceService.getInvoiceById(organizationId, invoiceId);
        return ResponseEntity.ok(InvoiceMapper.toResponse(invoice));
    }

    /**
     * Updates mutable fields of a draft invoice.
     * Only allowed when invoice is in DRAFT lifecycle status.
     */
    @PatchMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> updateDraftInvoice(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @Valid @RequestBody InvoiceUpdateRequest request
    ) {
        Invoice updated = invoiceService.updateInvoice(organizationId, invoiceId, request);
        return ResponseEntity.ok(InvoiceMapper.toResponse(updated));
    }

    /**
     * Hard-deletes a draft invoice. Only invoices in DRAFT lifecycle status may be deleted.
     */
    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<Void> deleteDraftInvoice(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId
    ) {
        invoiceService.deleteInvoice(organizationId, invoiceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets paginated invoices.
     */
    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getPaginatedInvoices(
        @PathVariable UUID organizationId,
        @RequestParam(required = false) TimeStatus timeStatus,
        @RequestParam(required = false) LifeCycleStatus lifeCycleStatus,
        @RequestParam(required = false) String invoiceNumber,
        @RequestParam(required = false) LocalDate createdAt,
        @RequestParam(required = false) LocalDate updatedAt,
        @RequestParam(required = false) LocalDate dueDate,
        Pageable pageable
    ){
        Page<Invoice> invoices = invoiceService.list(
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

    /**
     * Issue a draft invoice. Body may be omitted to issue with current date,
     * or may contain an explicit issueDate to apply.
     */
    @PostMapping("/{invoiceId}/issue")
    public ResponseEntity<InvoiceResponse> issueInvoice(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @Valid @RequestBody(required = false) IssueInvoiceRequest request
    ) {
        Invoice issued = invoiceService.issueInvoice(organizationId, invoiceId, request);
        return ResponseEntity.ok(InvoiceMapper.toResponse(issued));
    }

    /**
     * Cancels an ISSUED or PARTIALLY_PAID invoice.
     * Also cancels all pending follow-ups and closes any open confirmation link.
     */
    @PostMapping("/{invoiceId}/cancel")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId
    ) {
        Invoice cancelled = invoiceService.cancelInvoice(organizationId, invoiceId);
        followUpService.cancelPendingFollowUpsForInvoice(invoiceId);
        confirmationLinkService.closeForInvoice(invoiceId);
        return ResponseEntity.ok(InvoiceMapper.toResponse(cancelled));
    }

    /**
     * Downloads the invoice as a PDF file.
     */
    @GetMapping(value = "/{invoiceId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId
    ) {
        InvoicePdfFile pdfFile = invoiceService.generateInvoicePdf(organizationId, invoiceId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(pdfFile.fileName())
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfFile.content().length)
                .body(pdfFile.content());
    }
}
