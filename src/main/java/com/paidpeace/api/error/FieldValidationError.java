package com.paidpeace.api.error;

public record FieldValidationError(
    String field,
    String message
) {}