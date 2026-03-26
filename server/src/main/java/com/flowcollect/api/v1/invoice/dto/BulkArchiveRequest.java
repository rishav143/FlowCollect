package com.flowcollect.api.v1.invoice.dto;

import java.util.List;
import java.util.UUID;

public class BulkArchiveRequest {

    private List<UUID> invoiceIds;

    public List<UUID> getInvoiceIds() {
        return invoiceIds;
    }

    public void setInvoiceIds(List<UUID> invoiceIds) {
        this.invoiceIds = invoiceIds;
    }
}
