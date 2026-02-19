package com.cashclarity.api.error;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cashclarity.exception.base.AppException;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bean validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));

        return ResponseEntity.badRequest().body(Map.of(
                "code", "VALIDATION_ERROR",
                "errors", errors
        ));
    }

    // ALL business exceptions
    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(Map.of(
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }

    // unexpected bugs
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknown(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", "INTERNAL_ERROR",
                        "message", "Something went wrong"
                ));
    }
}
