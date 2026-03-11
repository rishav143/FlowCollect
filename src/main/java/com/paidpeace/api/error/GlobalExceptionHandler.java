package com.paidpeace.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.paidpeace.exception.base.AppException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bean validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
    
        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();
    
        ErrorResponse response = new ErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                Instant.now(),
                request.getRequestURI(),
                errors
        );
    
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // ALL business exceptions
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex,
            HttpServletRequest request
    ) {
        String code = ex.getCode() != null ? ex.getCode().toString() : ex.getClass().getSimpleName();

        ErrorResponse response = new ErrorResponse(
                code,
                ex.getMessage(),
                ex.getStatus().value(),
                Instant.now(),
                request.getRequestURI()
        );
    
        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    // unexpected bugs
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponse response = new ErrorResponse(
                ex.getClass().getSimpleName(),
                "Something went wrong",
                status.value(),
                Instant.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}