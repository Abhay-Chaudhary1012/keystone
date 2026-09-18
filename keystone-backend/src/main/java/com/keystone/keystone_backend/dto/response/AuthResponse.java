package com.keystone.keystone_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Response DTO returned after successful authentication.
 *
 * <p>Contains the JWT token and basic user information that the frontend needs
 * immediately after login (to display the user's name, role, etc. without
 * making a second API call).</p>
 *
 * <p><strong>WHY INCLUDE USER INFO IN THE LOGIN RESPONSE?</strong></p>
 * <p>After login, the React frontend typically needs to:</p>
 * <ul>
 *   <li>Store the JWT token (in memory or localStorage)</li>
 *   <li>Display the user's name in the header</li>
 *   <li>Route to the correct dashboard based on role (Dispatcher vs Customer etc.)</li>
 * </ul>
 * <p>Including this info in the login response avoids an extra GET /api/auth/me call.</p>
 *
 * <p><strong>SECURITY NOTE:</strong> The JWT token itself contains claims (role,
 * customerId, etc.) but the frontend should NOT parse the JWT to extract these.
 * Instead, the server returns them explicitly in this response. The JWT is opaque
 * to the frontend — it just stores and sends it in the Authorization header.</p>
 *
 * <p>{@code @JsonInclude(NON_NULL)} — when this DTO is reused for the /api/auth/me
 * endpoint, the token field is null and will be omitted from the JSON.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    /** The JWT token string (e.g., "eyJhbGciOiJIUzI1NiIs...") */
    private String token;

    /** Token type — always "Bearer" for JWT */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Database ID of the authenticated user */
    private Long userId;

    /** Authenticated user's username */
    private String username;

    /** Authenticated user's email */
    private String email;

    /** Authenticated user's display name */
    private String fullName;

    /** User's role: DISPATCHER, TECHNICIAN, MANAGER, or CUSTOMER */
    private String role;

    /** Customer org ID (only present for CUSTOMER-role users, null for internal staff) */
    private Long customerId;
}
