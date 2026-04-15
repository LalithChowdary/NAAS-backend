package com.naas.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for the NAAS backend.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} – fired when @Valid fails on a request body.
 *       Returns HTTP 400 with a map of field → error-message pairs.</li>
 *   <li>{@link RuntimeException} – fired by service layer for business-rule violations
 *       (duplicate email, not-found, etc.). Returns HTTP 400 with a single "message" key.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures (e.g. @NotBlank, @Email, @Pattern).
     * Returns { "fieldName": "error message", ... }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handles business-rule RuntimeExceptions thrown from service layer.
     * Returns { "message": "..." }
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
