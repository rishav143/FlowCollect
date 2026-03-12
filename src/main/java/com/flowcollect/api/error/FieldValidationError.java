package com.flowcollect.api.error;

public record FieldValidationError(
    String field,
    String message
) {}