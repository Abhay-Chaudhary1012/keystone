package com.keystone.keystone_backend.exception;

import com.keystone.keystone_backend.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for ALL controller-layer exceptions.
 *
 * <p><strong>HOW IT WORKS:</strong></p>
 * <p>{@code @RestControllerAdvice} combines {@code @ControllerAdvice} (intercepts
 * exceptions from all controllers) with {@code @ResponseBody} (returns JSON).
 * Each {@code @ExceptionHandler} method handles a specific exception type and
 * converts it to a standardized {@link ApiErrorResponse}.</p>
 *
 * <p><strong>WHAT THIS HANDLES vs WHAT SECURITY HANDLES:</strong></p>
 * <ul>
 *   <li>{@code GlobalExceptionHandler} — exceptions thrown FROM controller/service code
 *       (validation errors, business logic errors, not-found, login failures)</li>
 *   <li>{@code JwtAuthenticationEntryPoint} — 401 errors from the SECURITY FILTER CHAIN
 *       (missing/invalid JWT on protected endpoints)</li>
 *   <li>{@code CustomAccessDeniedHandler} — 403 errors from the SECURITY FILTER CHAIN
 *       (authenticated user lacks required role)</li>
 * </ul>
 *
 * <p><strong>INTERVIEW TIP:</strong> "How do you handle exceptions in Spring Boot?"
 * <br>→ Use {@code @RestControllerAdvice} with {@code @ExceptionHandler} methods.
 * Each handler maps an exception type to an HTTP status code and error response.
 * This keeps error handling centralized (DRY) instead of scattered across every
 * controller method with try-catch blocks.</p>
 *
 * <p><strong>IMPORTANT:</strong> Never expose stack traces, class names, or internal
 * details in API responses. An attacker could use these to discover the tech stack,
 * library versions, or internal structure.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==========================================
    // Authentication / Authorization (from controllers)
    // ==========================================

    /**
     * Handles invalid login credentials (wrong username or password).
     * HTTP 401 Unauthorized.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {

        log.warn("Authentication failed for request to {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // ==========================================
    // Resource Not Found
    // ==========================================

    /**
     * Handles requests for resources that don't exist.
     * HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ==========================================
    // Business Rule Violations
    // ==========================================

    /**
     * Handles business rule violations (e.g., editing immutable work orders,
     * site-customer ownership mismatch).
     * HTTP 409 Conflict.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleViolation(
            BusinessRuleException ex, HttpServletRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // ==========================================
    // Validation Errors
    // ==========================================

    /**
     * Handles Jakarta Bean Validation failures (e.g., @NotBlank, @Size, @Email).
     * HTTP 400 Bad Request.
     *
     * <p>Extracts individual field errors and returns them as a list.
     * Example response:</p>
     * <pre>
     * {
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "Validation failed",
     *   "validationErrors": ["username: must not be blank", "password: must not be blank"]
     * }
     * </pre>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Validation failed for {}: {}", request.getRequestURI(), errors);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles malformed JSON in request body.
     * HTTP 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed JSON in request to {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Malformed JSON request body", request);
    }

    // ==========================================
    // Catch-all for unexpected errors
    // ==========================================

    /**
     * Handles any unhandled exception.
     * HTTP 500 Internal Server Error.
     *
     * <p><strong>SECURITY:</strong> We do NOT include {@code ex.getMessage()} in the
     * response — it could contain SQL errors, class names, or internal details.
     * Instead, we return a generic message and log the full exception server-side.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error for request to {}: ", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", request);
    }

    // ==========================================
    // Helper
    // ==========================================

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status, String message, HttpServletRequest request) {

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
