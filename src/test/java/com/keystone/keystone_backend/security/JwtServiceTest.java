package com.keystone.keystone_backend.security;

import com.keystone.keystone_backend.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p><strong>TEST STRATEGY:</strong></p>
 * <p>These are pure unit tests — no Spring context, no database, no network.
 * We test the JWT logic in isolation using {@code ReflectionTestUtils} to inject
 * the secret key and expiration values that would normally come from
 * {@code @Value} annotations.</p>
 *
 * <p><strong>WHY NOT @SpringBootTest?</strong></p>
 * <ul>
 *   <li>@SpringBootTest starts the entire application context (slow, needs DB)</li>
 *   <li>JwtService has no dependencies that need Spring (just config values)</li>
 *   <li>Unit tests should be fast and isolated</li>
 * </ul>
 *
 * <p><strong>INTERVIEW TIP:</strong> "What's the difference between unit tests and
 * integration tests in Spring?"
 * <br>→ Unit tests (like this) test a single class in isolation, with dependencies
 * mocked or injected manually. They're fast (milliseconds).
 * Integration tests (@SpringBootTest) start the full application context and test
 * how components work together. They're slower but catch wiring/config issues.</p>
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal testDispatcher;
    private UserPrincipal testCustomer;

    /** Secret key for testing — must be at least 256 bits (32 bytes) for HS256 */
    private static final String TEST_SECRET =
            "keystone-test-secret-key-must-be-at-least-256-bits-for-hmac-sha256";

    /** Default expiration: 1 hour (3,600,000 ms) */
    private static final long TEST_EXPIRATION_MS = 3_600_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Inject config values that would normally come from @Value
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", TEST_EXPIRATION_MS);
        // Reset cached signing key so it rebuilds with the test secret
        ReflectionTestUtils.setField(jwtService, "signingKey", null);

        // Create test principals — no database involved
        testDispatcher = new UserPrincipal(
                1L, "dispatcher1", "dispatcher@keystone.com", "Diana Prince",
                "hashedPassword", Role.DISPATCHER, null, true,
                List.of(new SimpleGrantedAuthority("ROLE_DISPATCHER"))
        );

        testCustomer = new UserPrincipal(
                4L, "customer1", "customer@acme.com", "Bruce Wayne",
                "hashedPassword", Role.CUSTOMER, 1L, true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    // ==========================================
    // Token Generation Tests
    // ==========================================
    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("should generate a non-null, non-empty JWT token")
        void generateToken_returnsValidJwt() {
            String token = jwtService.generateToken(testDispatcher);

            assertNotNull(token, "Token should not be null");
            assertFalse(token.isBlank(), "Token should not be blank");
            // JWT has 3 parts separated by dots: header.payload.signature
            assertEquals(3, token.split("\\.").length, "JWT should have 3 parts");
        }

        @Test
        @DisplayName("should include userId in token claims")
        void generateToken_containsUserId() {
            String token = jwtService.generateToken(testDispatcher);
            Claims claims = jwtService.extractAllClaims(token);

            assertEquals(1, claims.get("userId", Integer.class));
        }

        @Test
        @DisplayName("should include role in token claims")
        void generateToken_containsRole() {
            String token = jwtService.generateToken(testDispatcher);
            Claims claims = jwtService.extractAllClaims(token);

            assertEquals("DISPATCHER", claims.get("role", String.class));
        }

        @Test
        @DisplayName("should include customerId for CUSTOMER users")
        void generateToken_containsCustomerId_forCustomerRole() {
            String token = jwtService.generateToken(testCustomer);
            Claims claims = jwtService.extractAllClaims(token);

            assertEquals(1, claims.get("customerId", Integer.class));
        }

        @Test
        @DisplayName("should NOT include customerId for internal staff")
        void generateToken_omitsCustomerId_forInternalStaff() {
            String token = jwtService.generateToken(testDispatcher);
            Claims claims = jwtService.extractAllClaims(token);

            assertNull(claims.get("customerId"),
                    "Internal staff should not have customerId in JWT");
        }
    }

    // ==========================================
    // Username Extraction Tests
    // ==========================================
    @Nested
    @DisplayName("Username Extraction")
    class UsernameExtraction {

        @Test
        @DisplayName("should extract the correct username from token")
        void extractUsername_returnsCorrectUsername() {
            String token = jwtService.generateToken(testDispatcher);
            String username = jwtService.extractUsername(token);

            assertEquals("dispatcher1", username);
        }

        @Test
        @DisplayName("should extract different usernames for different users")
        void extractUsername_differsByUser() {
            String dispatcherToken = jwtService.generateToken(testDispatcher);
            String customerToken = jwtService.generateToken(testCustomer);

            assertNotEquals(
                    jwtService.extractUsername(dispatcherToken),
                    jwtService.extractUsername(customerToken)
            );
        }
    }

    // ==========================================
    // Token Validation Tests
    // ==========================================
    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("should validate token for the correct user")
        void isTokenValid_withCorrectUser_returnsTrue() {
            String token = jwtService.generateToken(testDispatcher);
            assertTrue(jwtService.isTokenValid(token, testDispatcher));
        }

        @Test
        @DisplayName("should reject token for a different user")
        void isTokenValid_withDifferentUser_returnsFalse() {
            String token = jwtService.generateToken(testDispatcher);
            // Token was generated for dispatcher1, but we validate against customer1
            assertFalse(jwtService.isTokenValid(token, testCustomer));
        }

        @Test
        @DisplayName("should reject expired token")
        void isTokenValid_withExpiredToken_returnsFalse() {
            // Set expiration to 1 millisecond
            ReflectionTestUtils.setField(jwtService, "expirationMs", 1L);
            ReflectionTestUtils.setField(jwtService, "signingKey", null);

            String token = jwtService.generateToken(testDispatcher);

            // Wait for token to expire
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}

            assertFalse(jwtService.isTokenValid(token, testDispatcher),
                    "Expired token should not be valid");
        }

        @Test
        @DisplayName("should reject tampered token")
        void isTokenValid_withTamperedToken_returnsFalse() {
            String token = jwtService.generateToken(testDispatcher);
            String tampered = token + "xyz";

            assertFalse(jwtService.isTokenValid(tampered, testDispatcher),
                    "Tampered token should not be valid");
        }

        @Test
        @DisplayName("should reject null token")
        void isTokenValid_withNullToken_returnsFalse() {
            assertFalse(jwtService.isTokenValid(null, testDispatcher));
        }

        @Test
        @DisplayName("should reject empty token")
        void isTokenValid_withEmptyToken_returnsFalse() {
            assertFalse(jwtService.isTokenValid("", testDispatcher));
        }

        @Test
        @DisplayName("should reject random string as token")
        void isTokenValid_withRandomString_returnsFalse() {
            assertFalse(jwtService.isTokenValid("not.a.jwt", testDispatcher));
        }
    }

    // ==========================================
    // Token Expiry Tests
    // ==========================================
    @Nested
    @DisplayName("Token Expiry")
    class TokenExpiry {

        @Test
        @DisplayName("should throw ExpiredJwtException for expired token")
        void extractUsername_withExpiredToken_throwsExpiredJwtException() {
            ReflectionTestUtils.setField(jwtService, "expirationMs", 1L);
            ReflectionTestUtils.setField(jwtService, "signingKey", null);

            String token = jwtService.generateToken(testDispatcher);

            try { Thread.sleep(50); } catch (InterruptedException ignored) {}

            assertThrows(ExpiredJwtException.class,
                    () -> jwtService.extractUsername(token),
                    "Expired token should throw ExpiredJwtException");
        }
    }

    // ==========================================
    // Password Encoder Test (bonus — not JwtService, but critical)
    // ==========================================
    @Nested
    @DisplayName("BCrypt Password Encoding")
    class PasswordEncoding {

        @Test
        @DisplayName("should verify BCrypt password matching works")
        void bcryptPasswordMatching() {
            var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            String rawPassword = "password123";
            String hash = encoder.encode(rawPassword);

            assertTrue(encoder.matches(rawPassword, hash),
                    "BCrypt should match the original password");
            assertFalse(encoder.matches("wrongPassword", hash),
                    "BCrypt should reject wrong password");
        }

        @Test
        @DisplayName("should produce different hashes for same password (salt)")
        void bcryptProducesDifferentHashes() {
            var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            String hash1 = encoder.encode("password123");
            String hash2 = encoder.encode("password123");

            // BCrypt includes a random salt, so hashes differ even for the same password
            assertNotEquals(hash1, hash2,
                    "BCrypt should produce different hashes due to unique salts");
            // But both should still match the original password
            assertTrue(encoder.matches("password123", hash1));
            assertTrue(encoder.matches("password123", hash2));
        }
    }
}
