package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.dto.request.LoginRequest;
import com.keystone.keystone_backend.dto.response.AuthResponse;
import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.exception.InvalidCredentialsException;
import com.keystone.keystone_backend.repository.UserRepository;
import com.keystone.keystone_backend.security.JwtService;
import com.keystone.keystone_backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user authentication (login).
 *
 * <p><strong>LOGIN FLOW:</strong></p>
 * <pre>
 * LoginRequest(username, password)
 *     │
 *     ▼
 * 1. Find user by username in DB
 *     │ (not found → InvalidCredentialsException)
 *     ▼
 * 2. BCrypt.matches(rawPassword, storedHash)
 *     │ (mismatch → InvalidCredentialsException)
 *     ▼
 * 3. Check user.active flag
 *     │ (inactive → InvalidCredentialsException)
 *     ▼
 * 4. Create UserPrincipal from User entity
 *     │
 *     ▼
 * 5. Generate JWT token via JwtService
 *     │
 *     ▼
 * 6. Return AuthResponse (token + user info)
 * </pre>
 *
 * <p><strong>WHY MANUAL AUTH INSTEAD OF AuthenticationManager?</strong></p>
 * <p>Spring Security's {@code AuthenticationManager.authenticate()} delegates to
 * {@code DaoAuthenticationProvider} which calls our {@code CustomUserDetailsService}
 * and {@code PasswordEncoder}. If authentication fails, it throws
 * {@code AuthenticationException} — which Spring Security's
 * {@code ExceptionTranslationFilter} intercepts BEFORE our
 * {@code GlobalExceptionHandler} can handle it, producing a different error format.</p>
 * <p>Manual auth gives us full control over error responses. In an interview, you should
 * know both approaches — explain that AuthenticationManager is the standard Spring
 * Security pattern, but we chose manual auth for consistent JSON error responses.</p>
 *
 * <p><strong>SECURITY: SAME ERROR MESSAGE FOR ALL FAILURES</strong></p>
 * <p>We return "Invalid username or password" whether the username doesn't exist OR
 * the password is wrong. This prevents <strong>username enumeration attacks</strong>
 * — an attacker cannot determine valid usernames by observing different responses.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request The login request containing username and password
     * @return AuthResponse with JWT token and user information
     * @throws InvalidCredentialsException if credentials are invalid or account is inactive
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        // Step 1: Find user by username
        // Same error message for "not found" and "wrong password" prevents enumeration
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login attempt for non-existent username: {}", request.getUsername());
                    return new InvalidCredentialsException();
                });

        // Step 2: Verify password using BCrypt
        // passwordEncoder.matches(rawPassword, storedBCryptHash) does:
        // 1. Extract the salt from the stored hash
        // 2. Hash the raw password with that same salt
        // 3. Compare the result with the stored hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            throw new InvalidCredentialsException();
        }

        // Step 3: Check if account is active
        if (!user.getActive()) {
            log.warn("Login attempt for deactivated account: {}", request.getUsername());
            throw new InvalidCredentialsException("Account is deactivated");
        }

        // Step 4: Create UserPrincipal (adapter for Spring Security)
        UserPrincipal principal = UserPrincipal.from(user);

        // Step 5: Generate JWT token
        String token = jwtService.generateToken(principal);

        log.info("Successful login for user: {} with role: {}", user.getUsername(), user.getRole());

        // Step 6: Build and return response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .customerId(user.getCustomer() != null ? user.getCustomer().getId() : null)
                .build();
    }
}
