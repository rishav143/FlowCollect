package com.paidpeace.application.invoice;

public record InvoicePdfFile(
        byte[] content,
        String fileName
) {
}
