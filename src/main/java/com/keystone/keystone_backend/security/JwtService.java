package com.keystone.keystone_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for all JWT token operations: generation, validation, claim extraction.
 *
 * <p><strong>HOW JWT WORKS (Interview-critical):</strong></p>
 * <p>A JWT has three parts separated by dots: {@code header.payload.signature}</p>
 * <ul>
 *   <li><strong>Header:</strong> Algorithm (HS256) and token type (JWT)</li>
 *   <li><strong>Payload:</strong> Claims — data stored in the token (username, role, expiry)</li>
 *   <li><strong>Signature:</strong> HMAC-SHA256(header + payload, secret_key)</li>
 * </ul>
 * <p>The signature ensures the token hasn't been tampered with. If anyone changes the
 * payload (e.g., changes role from CUSTOMER to MANAGER), the signature won't match
 * and the server will reject it.</p>
 *
 * <p><strong>WHY STORE ROLE AND CUSTOMERID IN JWT CLAIMS?</strong></p>
 * <p>While we load UserDetails from the database on every request (for security),
 * storing role and customerId in the JWT enables:</p>
 * <ul>
 *   <li>The frontend to read the role from the login response (for routing)</li>
 *   <li>Future optimization: skip DB lookup for read-only endpoints</li>
 *   <li>Audit logging: JWT claims tell us who made the request even if DB is down</li>
 * </ul>
 *
 * <p><strong>JJWT LIBRARY (0.12.x):</strong></p>
 * <p>Uses a split-module design:</p>
 * <ul>
 *   <li>{@code jjwt-api} — compile-time API (you import from this)</li>
 *   <li>{@code jjwt-impl} — runtime implementation (never import directly)</li>
 *   <li>{@code jjwt-jackson} — JSON serialization (runtime)</li>
 * </ul>
 * <p>This separation means your code depends only on the stable API, not the implementation.</p>
 *
 * <p><strong>INTERVIEW TIP:</strong> "What happens if someone steals a JWT?"
 * <br>→ They can impersonate the user until the token expires. This is why:
 * (1) tokens have short expiration times, (2) the secret key must be kept secure,
 * (3) tokens should be transmitted over HTTPS only, and (4) sensitive actions
 * should re-verify credentials.</p>
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /** Cached signing key — created lazily on first use */
    private SecretKey signingKey;

    /**
     * Generates a JWT token for an authenticated user.
     *
     * <p>The token contains:</p>
     * <ul>
     *   <li>{@code sub} (subject) — the username (used to identify the user)</li>
     *   <li>{@code userId} — database ID</li>
     *   <li>{@code role} — the user's role (DISPATCHER, TECHNICIAN, etc.)</li>
     *   <li>{@code email} — the user's email</li>
     *   <li>{@code customerId} — customer org ID (only for CUSTOMER role)</li>
     *   <li>{@code iat} (issued at) — when the token was created</li>
     *   <li>{@code exp} (expiration) — when the token expires</li>
     * </ul>
     */
    public String generateToken(UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userPrincipal.getId());
        claims.put("role", userPrincipal.getRole().name());
        claims.put("email", userPrincipal.getEmail());
        if (userPrincipal.getCustomerId() != null) {
            claims.put("customerId", userPrincipal.getCustomerId());
        }

        return Jwts.builder()
                .claims(claims)                                               // Custom claims
                .subject(userPrincipal.getUsername())                          // Standard 'sub' claim
                .issuedAt(new Date())                                         // Standard 'iat' claim
                .expiration(new Date(System.currentTimeMillis() + expirationMs)) // Standard 'exp' claim
                .signWith(getSigningKey())                                     // Sign with HMAC-SHA key
                .compact();                                                    // Build the JWT string
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * <p>This method also VALIDATES the token — JJWT automatically checks:</p>
     * <ul>
     *   <li>Signature validity (tamper detection)</li>
     *   <li>Token expiration</li>
     * </ul>
     * <p>If either check fails, a {@link JwtException} is thrown.</p>
     *
     * @throws ExpiredJwtException   if the token has expired
     * @throws MalformedJwtException if the token format is invalid
     * @throws io.jsonwebtoken.security.SignatureException if the signature doesn't match
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Validates a JWT token against a UserDetails object.
     *
     * <p>Checks that:</p>
     * <ol>
     *   <li>The token can be parsed without errors (signature, format, expiry)</li>
     *   <li>The token's subject matches the UserDetails username</li>
     * </ol>
     *
     * @return true if the token is valid for the given user, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername());
            // Note: JJWT already validates expiration during extractAllClaims()
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parses the JWT and returns all claims (payload data).
     *
     * <p>JJWT performs full validation during parsing:</p>
     * <ul>
     *   <li>{@code verifyWith(key)} — verifies the signature matches</li>
     *   <li>Expiration check — rejects expired tokens automatically</li>
     * </ul>
     *
     * <p>Package-private visibility — used by this class and accessible in tests.</p>
     */
    Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())     // Set the key to verify signature
                .build()                          // Build the parser
                .parseSignedClaims(token)         // Parse + validate + return
                .getPayload();                    // Get the claims (payload)
    }

    /**
     * Returns the HMAC-SHA signing key derived from the configured secret string.
     *
     * <p><strong>Key requirements for HS256:</strong></p>
     * <ul>
     *   <li>Minimum 256 bits (32 bytes) — JJWT enforces this</li>
     *   <li>Our default dev key is 58 chars = 464 bits (sufficient)</li>
     *   <li>In production: use a cryptographically random key via env variable</li>
     * </ul>
     *
     * <p>Lazy initialization: the key is created on first use, after Spring injects
     * the {@code @Value} fields. This also works in tests where fields are set
     * via {@code ReflectionTestUtils}.</p>
     */
    private SecretKey getSigningKey() {
        if (signingKey == null) {
            signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return signingKey;
    }
}
