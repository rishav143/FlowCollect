package com.flowcollect.api.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp,
        String path,
        List<FieldValidationError> errors
) {
    public ErrorResponse(String code, String message, int status, Instant timestamp, String path, List<FieldValidationError> errors) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
        this.path = path;
        this.errors = errors;
    }
    public ErrorResponse(String code, String message, int status, Instant timestamp, String path) {
        this(code, message, status, timestamp, path, List.of());
    }
}