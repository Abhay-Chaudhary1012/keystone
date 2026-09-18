package com.keystone.keystone_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response returned by ALL error-handling paths in the application.
 *
 * <p><strong>WHY A CONSISTENT ERROR FORMAT?</strong></p>
 * <ul>
 *   <li>The React frontend can rely on a single error shape for ALL API errors</li>
 *   <li>No guessing whether the error is a string, object, or something else</li>
 *   <li>Matches Spring Boot's default error format (status, error, message, path)
 *       so it feels familiar</li>
 * </ul>
 *
 * <p>This DTO is used by:</p>
 * <ul>
 *   <li>{@code GlobalExceptionHandler} — for exceptions thrown from controllers/services</li>
 *   <li>{@code JwtAuthenticationEntryPoint} — for 401 errors (unauthenticated)</li>
 *   <li>{@code CustomAccessDeniedHandler} — for 403 errors (insufficient role)</li>
 * </ul>
 *
 * <p><strong>INTERVIEW TIP:</strong> "Why not just use Spring Boot's default error response?"
 * <br>→ The default format includes implementation details like exception class names
 * and stack traces. A custom error DTO lets you control exactly what's exposed.
 * Never leak internal details to API consumers.</p>
 *
 * <p><strong>Annotation:</strong> {@code @JsonInclude(NON_NULL)} — fields that are null
 * are omitted from the JSON response. For example, {@code validationErrors} is only
 * present when there ARE validation errors.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /** HTTP status code (e.g., 401, 403, 404, 500) */
    private int status;

    /** HTTP status reason phrase (e.g., "Unauthorized", "Not Found") */
    private String error;

    /** Human-readable error message */
    private String message;

    /** When the error occurred */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** The request path that caused the error */
    private String path;

    /**
     * Field-level validation errors (only present for 400 validation failures).
     * Example: ["title: must not be blank", "priority: must not be null"]
     */
    private List<String> validationErrors;
}
