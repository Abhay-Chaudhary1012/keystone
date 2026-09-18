package com.keystone.keystone_backend.controller;

import com.keystone.keystone_backend.dto.request.LoginRequest;
import com.keystone.keystone_backend.dto.response.AuthResponse;
import com.keystone.keystone_backend.security.UserPrincipal;
import com.keystone.keystone_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 *
 * <p><strong>ENDPOINTS:</strong></p>
 * <ul>
 *   <li>{@code POST /api/auth/login} — Authenticate and receive JWT (public)</li>
 *   <li>{@code GET /api/auth/me} — Get current authenticated user info (protected)</li>
 * </ul>
 *
 * <p><strong>THIN CONTROLLER PATTERN:</strong></p>
 * <p>This controller contains NO business logic. It only:</p>
 * <ol>
 *   <li>Receives the HTTP request</li>
 *   <li>Validates input ({@code @Valid})</li>
 *   <li>Delegates to the service layer</li>
 *   <li>Returns the response with appropriate HTTP status</li>
 * </ol>
 * <p>All business logic (credential verification, token generation) lives in
 * {@link AuthService}. This separation makes the code testable and maintainable.</p>
 *
 * <p><strong>@RestController:</strong> Combines {@code @Controller} + {@code @ResponseBody}.
 * Every method return value is serialized to JSON automatically.</p>
 *
 * <p><strong>@RequestMapping("/api/auth"):</strong> Base path for all endpoints in this
 * controller. Individual methods add their path (e.g., "/login" → "/api/auth/login").</p>
 *
 * <p><strong>INTERVIEW TIP:</strong> "Why use ResponseEntity instead of returning
 * the object directly?"
 * <br>→ {@code ResponseEntity} gives you control over the HTTP status code and headers.
 * Returning an object directly always returns 200 OK. With ResponseEntity, you can
 * return 201 Created, 204 No Content, or set custom headers.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and user authentication endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p><strong>Public endpoint</strong> — no JWT required (configured in SecurityConfig).</p>
     *
     * <p>Request example:</p>
     * <pre>
     * POST /api/auth/login
     * Content-Type: application/json
     *
     * {
     *   "username": "dispatcher1",
     *   "password": "password123"
     * }
     * </pre>
     *
     * <p>Success response (200 OK):</p>
     * <pre>
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIs...",
     *   "tokenType": "Bearer",
     *   "userId": 1,
     *   "username": "dispatcher1",
     *   "email": "dispatcher@keystone.com",
     *   "fullName": "Diana Prince",
     *   "role": "DISPATCHER"
     * }
     * </pre>
     *
     * @param request Login credentials (username + password)
     * @return JWT token and user information
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with username and password to receive a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (missing username or password)"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // @Valid triggers Jakarta Bean Validation on LoginRequest
        // If validation fails → MethodArgumentNotValidException → GlobalExceptionHandler → 400
        // If credentials invalid → InvalidCredentialsException → GlobalExceptionHandler → 401
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the currently authenticated user's information.
     *
     * <p><strong>Protected endpoint</strong> — requires a valid JWT in the Authorization header.</p>
     *
     * <p>This endpoint is useful for the React frontend to:</p>
     * <ul>
     *   <li>Verify the JWT is still valid (not expired)</li>
     *   <li>Refresh user info (role, name) after page reload</li>
     *   <li>Check if the user is still active</li>
     * </ul>
     *
     * <p><strong>@AuthenticationPrincipal:</strong> Spring Security injects the UserPrincipal
     * from the SecurityContext. This is the same object set by JwtAuthenticationFilter.</p>
     *
     * <p>Request example:</p>
     * <pre>
     * GET /api/auth/me
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
     * </pre>
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the authenticated user's profile information",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated (missing or invalid JWT)")
    })
    public ResponseEntity<AuthResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {

        // Build response from the UserPrincipal already loaded by JwtAuthenticationFilter
        // No service call needed — the data is already in the SecurityContext
        AuthResponse response = AuthResponse.builder()
                .userId(principal.getId())
                .username(principal.getUsername())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .role(principal.getRole().name())
                .customerId(principal.getCustomerId())
                .build();
        // Note: token is null here (not included in response via @JsonInclude(NON_NULL))

        return ResponseEntity.ok(response);
    }
}
