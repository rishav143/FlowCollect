package com.cashclarity.api.v1.invoice;

import com.cashclarity.api.v1.invoice.dto.InvoiceItemResponse;
import com.cashclarity.api.v1.invoice.dto.InvoiceResponse;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.invoice.InvoiceItem;

import java.util.List;

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
        response.setTaxPercentage(invoice.getTaxInPercentage());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setItems(toItemResponses(invoice.getItems()));
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());
        return response;
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
