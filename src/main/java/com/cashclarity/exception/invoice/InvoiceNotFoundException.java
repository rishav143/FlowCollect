package com.cashclarity.exception.invoice;

import com.cashclarity.exception.CashClarityException;

public class InvoiceNotFoundException extends CashClarityException {
    public InvoiceNotFoundException(Long id) {
        super("Invoice not found for id '" + id + "'.");
    }
    
}
