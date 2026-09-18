package com.keystone.keystone_backend.exception;

/**
 * Thrown when login credentials are invalid (wrong username or wrong password).
 *
 * <p>Handled by {@code GlobalExceptionHandler} → returns HTTP 401 Unauthorized.</p>
 *
 * <p><strong>SECURITY NOTE — WHY THE SAME MESSAGE FOR BOTH CASES?</strong></p>
 * <p>We intentionally use the same error message ("Invalid username or password")
 * whether the username doesn't exist OR the password is wrong. This prevents
 * <strong>username enumeration attacks</strong> — an attacker cannot determine
 * which usernames are valid by observing different error messages.</p>
 *
 * <p><strong>IMPORTANT:</strong> This exception does NOT extend Spring Security's
 * {@code AuthenticationException}. If it did, Spring Security's
 * {@code ExceptionTranslationFilter} would intercept it before our
 * {@code GlobalExceptionHandler} could handle it, producing a different
 * error response format. By extending plain {@code RuntimeException},
 * we ensure our centralized error handler controls the response.</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
