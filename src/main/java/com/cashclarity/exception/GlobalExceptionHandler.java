package com.cashclarity.exception;

import com.cashclarity.exception.organization.*;
import com.cashclarity.exception.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles exceptions thrown from controller and service layers.
 * Converts validation errors and custom business exceptions into
 * consistent HTTP responses with appropriate status codes and messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation (e.g. @Valid) errors on request DTOs.
     * Returns 400 Bad Request with field-level error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "message", "Validation failed.",
                        "errors", errors
                ));
    }

    /**
     * Handles duplicate organization email (409 Conflict).
     */
    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationAlreadyExists(OrganizationAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles duplicate user email in an organization (409 Conflict).
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles user already active (409 Conflict).
     */
    @ExceptionHandler(UserAlreadyActiveException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyActive(UserAlreadyActiveException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles user already inactive (409 Conflict).
     */
    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyInactive(UserAlreadyInactiveException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles organization already archived (409 Conflict).
     */
    @ExceptionHandler(OrganizationAlreadyArchivedException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationAlreadyArchived(OrganizationAlreadyArchivedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles organization already active (409 Conflict).
     */
    @ExceptionHandler(OrganizationAlreadyActiveException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationAlreadyActive(OrganizationAlreadyActiveException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles organization already suspended (409 Conflict).
     */
    @ExceptionHandler(OrganizationAlreadySuspendedException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationAlreadySuspended(OrganizationAlreadySuspendedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles organization already in trial (409 Conflict).
     */
    @ExceptionHandler(OrganizationAlreadyInTrialException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationAlreadyInTrial(OrganizationAlreadyInTrialException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles organization already expired (409 Conflict).
     */
    @ExceptionHandler(OrganizationAlreadyExpiredException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationAlreadyExpired(OrganizationAlreadyExpiredException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles organization logo not found (404 Not Found).
     */
    @ExceptionHandler(OrganizationLogoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationLogoNotFound(OrganizationLogoNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles invalid input (400 Bad Request).
     */
    @ExceptionHandler({
            InvalidTimezoneException.class,
            InvalidCurrencyException.class,
            InvalidOrganizationIdException.class,
            InvalidOrganizationFieldException.class,
            InvalidUserFieldException.class,
            InvalidUserIdException.class
    })
    public ResponseEntity<Map<String, String>> handleInvalidInput(CashClarityException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles organization not found (404 Not Found).
     */
    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationNotFound(OrganizationNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles user not found (404 Not Found).
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles any other custom application exception.
     */
    @ExceptionHandler(CashClarityException.class)
    public ResponseEntity<Map<String, String>> handleCashClarityException(CashClarityException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}
