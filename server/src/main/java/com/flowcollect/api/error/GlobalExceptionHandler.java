package com.flowcollect.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.flowcollect.exception.base.AppException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponse response = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
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