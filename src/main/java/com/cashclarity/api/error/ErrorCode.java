package com.cashclarity.api.error;

/**
 * Stable machine-readable API error identifiers.
 * This is part of API contract — never change values once released.
 */
public enum ErrorCode {

    // -------- CUSTOMER --------
    CUSTOMER_NOT_FOUND,
    CUSTOMER_ALREADY_EXISTS,
    INVALID_CUSTOMER_FIELD,

    // -------- ORGANIZATION --------
    ORGANIZATION_NOT_FOUND,
    ORGANIZATION_ALREADY_EXISTS,
    INVALID_ORGANIZATION_FIELD,

    // -------- USER --------
    USER_NOT_FOUND,
    USER_ALREADY_EXISTS,
    INVALID_USER_FIELD,

    // -------- TEMPLATE --------
    TEMPLATE_NOT_FOUND,
    TEMPLATE_ALREADY_EXISTS,
    INVALID_TEMPLATE_FIELD,

    // -------- INVOICE --------
    INVOICE_NOT_FOUND,
    INVOICE_ALREADY_EXISTS,
    INVALID_INVOICE_FIELD,

    // -------- PAYMENT --------
    PAYMENT_NOT_FOUND,
    PAYMENT_ALREADY_EXISTS,
    INVALID_PAYMENT_FIELD,

    // -------- REMINDER --------
    REMINDER_NOT_FOUND,
    REMINDER_ALREADY_EXISTS,
    INVALID_REMINDER_FIELD,

    // -------- FOLLOW_UP --------
    FOLLOW_UP_NOT_FOUND,
    FOLLOW_UP_ALREADY_EXISTS,
    INVALID_FOLLOW_UP_FIELD,

    // -------- SYSTEM --------
    INTERNAL_ERROR
}
