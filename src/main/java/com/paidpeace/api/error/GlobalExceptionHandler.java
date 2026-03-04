package com.paidpeace.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.paidpeace.exception.base.AppException;

import java.time.Instant;
import java.util.stream.Collectors;

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
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));

        ErrorResponse response = new ErrorResponse(
                "VALIDATION_ERROR",
                errors.toString(),
                HttpStatus.BAD_REQUEST.value(),
                Instant.now(),
                request.getRequestURI()
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

        ErrorResponse response = new ErrorResponse(
                ex.getCode().toString(),
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

        ErrorResponse response = new ErrorResponse(
                "INTERNAL_ERROR",
                "Something went wrong",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Instant.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}