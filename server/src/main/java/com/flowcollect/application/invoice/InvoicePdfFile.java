package com.flowcollect.application.invoice;

public record InvoicePdfFile(
        byte[] content,
        String fileName
) {
}
