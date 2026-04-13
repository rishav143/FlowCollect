package com.flowcollect.api.v1.invoice;

import java.util.List;

import com.flowcollect.api.v1.invoice.dto.InvoiceItemResponse;
import com.flowcollect.api.v1.invoice.dto.InvoiceResponse;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.InvoiceItem;
import com.flowcollect.domain.invoice.InvoiceTaxLine;

public final class InvoiceMapper {

    private InvoiceMapper() {
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setOrganizationId(invoice.getOrganization() != null ? invoice.getOrganization().getId() : null);
        response.setCustomerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null);
        response.setCreatedByUserId(invoice.getCreatedBy() != null ? invoice.getCreatedBy().getId() : null);
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setTimeStatus(invoice.getTimeStatus());
        response.setLifeCycleStatus(invoice.getLifeCycleStatus());
        response.setIssueDate(invoice.getIssueDate());
        response.setDueDate(invoice.getDueDate());
        response.setSubtotal(invoice.getSubtotal());
        response.setDiscountAmount(invoice.getDiscountAmount());
        response.setTaxLines(toTaxLineResponses(invoice.getTaxLines()));
        response.setTotalAmount(invoice.getTotalAmount());
        response.setTotalPaid(invoice.getTotalPaid());
        response.setRemainingAmount(invoice.getRemainingAmount());
        response.setItems(toItemResponses(invoice.getItems()));
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());
        return response;
    }

    private static List<InvoiceResponse.TaxLineResponse> toTaxLineResponses(List<InvoiceTaxLine> taxLines) {
        return taxLines.stream().map(tl -> {
            InvoiceResponse.TaxLineResponse r = new InvoiceResponse.TaxLineResponse();
            r.setLabel(tl.getLabel());
            r.setAmount(tl.getAmount());
            return r;
        }).toList();
    }

    private static List<InvoiceItemResponse> toItemResponses(List<InvoiceItem> items) {
        return items.stream().map(InvoiceMapper::toItemResponse).toList();
    }

    private static InvoiceItemResponse toItemResponse(InvoiceItem item) {
        InvoiceItemResponse response = new InvoiceItemResponse();
        response.setId(item.getId());
        response.setDescription(item.getDescription());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setAmount(item.getAmount());
        return response;
    }
}
