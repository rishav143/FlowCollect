package com.flowcollect.api.v1.confirmation;

import com.flowcollect.api.v1.confirmation.dto.CustomerConfirmationView;
import com.flowcollect.api.v1.confirmation.dto.PaymentConfirmationResponse;
import com.flowcollect.api.v1.confirmation.dto.PaymentConfirmationSubmitResponse;
import com.flowcollect.domain.confirmation.ConfirmationLink;
import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.invoice.Invoice;

public final class PaymentConfirmationMapper {

    private PaymentConfirmationMapper() {}

    /**
     * Builds the public customer-facing view of a confirmation link.
     * No internal IDs are exposed — only the information the customer needs to
     * confirm they are paying the right invoice.
     */
    public static CustomerConfirmationView toCustomerView(ConfirmationLink link) {
        Invoice invoice = link.getInvoice();
        CustomerConfirmationView view = new CustomerConfirmationView();
        view.setInvoiceNumber(invoice.getInvoiceNumber());
        view.setOrganizationName(invoice.getOrganization().getName());
        view.setCustomerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : null);
        view.setTotalAmount(invoice.getTotalAmount());
        view.setTotalPaid(invoice.getTotalPaid());
        view.setRemainingAmount(invoice.getRemainingAmount());
        view.setDueDate(invoice.getDueDate());
        view.setCurrency(invoice.getOrganization().getCurrency().getCurrencyCode());
        view.setLinkStatus(link.getStatus().name());
        return view;
    }

    /** Slim response returned to the customer immediately after they submit a claim. */
    public static PaymentConfirmationSubmitResponse toSubmitResponse(PaymentConfirmation confirmation) {
        PaymentConfirmationSubmitResponse response = new PaymentConfirmationSubmitResponse();
        response.setConfirmationId(confirmation.getId());
        response.setAmountClaimed(confirmation.getAmountClaimed());
        response.setStatus(confirmation.getStatus().name());
        response.setCreatedAt(confirmation.getCreatedAt());
        return response;
    }

    /** Full response returned to authenticated business users. */
    public static PaymentConfirmationResponse toResponse(PaymentConfirmation confirmation) {
        ConfirmationLink link = confirmation.getConfirmationLink();
        Invoice invoice = link.getInvoice();

        PaymentConfirmationResponse response = new PaymentConfirmationResponse();
        response.setId(confirmation.getId());
        response.setInvoiceId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setConfirmationLinkId(link.getId());
        response.setAmountClaimed(confirmation.getAmountClaimed());
        response.setInvoiceTotalAmount(invoice.getTotalAmount());
        response.setInvoiceRemainingAmount(invoice.getRemainingAmount());
        response.setCustomerNote(confirmation.getCustomerNote());
        response.setStatus(confirmation.getStatus());
        response.setBusinessNote(confirmation.getBusinessNote());
        response.setReviewedAt(confirmation.getReviewedAt());
        response.setCreatedAt(confirmation.getCreatedAt());
        return response;
    }
}
