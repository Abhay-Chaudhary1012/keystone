package com.keystone.keystone_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — the heart of the stateless auth system.
 *
 * <p><strong>HOW IT WORKS (request flow):</strong></p>
 * <pre>
 * HTTP Request
 *     │
 *     ▼
 * ┌─────────────────────────────────┐
 * │ JwtAuthenticationFilter         │
 * │  1. Check Authorization header  │
 * │  2. Extract JWT token           │
 * │  3. Parse & validate token      │
 * │  4. Load user from DB           │
 * │  5. Set SecurityContext         │
 * └────────────┬────────────────────┘
 *              │
 *              ▼
 * ┌─────────────────────────────────┐
 * │ Spring Security Filters         │
 * │  (ExceptionTranslationFilter,   │
 * │   AuthorizationFilter)          │
 * └────────────┬────────────────────┘
 *              │
 *              ▼
 * ┌─────────────────────────────────┐
 * │ DispatcherServlet → Controller  │
 * └─────────────────────────────────┘
 * </pre>
 *
 * <p><strong>WHY OncePerRequestFilter?</strong></p>
 * <p>Extends {@code OncePerRequestFilter} instead of plain {@code Filter} to guarantee
 * this filter runs exactly ONCE per request. In some servlet configurations (e.g., with
 * forwards or includes), a plain Filter could run multiple times.</p>
 *
 * <p><strong>WHY LOAD FROM DATABASE ON EVERY REQUEST?</strong></p>
 * <p>We could trust the JWT claims (role, customerId) without hitting the DB. But loading
 * from DB on each request ensures:</p>
 * <ul>
 *   <li>Deactivated users are immediately locked out (even if JWT hasn't expired)</li>
 *   <li>Role changes take effect immediately (not just on next login)</li>
 *   <li>The SecurityContext always has fresh, authoritative user data</li>
 * </ul>
 * <p>The tradeoff is one DB query per authenticated request. For an internship project,
 * correctness is more important than micro-optimization.</p>
 *
 * <p><strong>INTERVIEW TIP:</strong> "How does Spring Security know who is making
 * the request?"
 * <br>→ This filter sets a {@code UsernamePasswordAuthenticationToken} into the
 * {@code SecurityContextHolder}. All downstream code (controllers, services,
 * {@code @PreAuthorize} checks) reads from this context. If no Authentication is
 * set, the request is treated as anonymous/unauthenticated.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Extract the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Step 2: If no Bearer token, skip authentication and continue the filter chain.
        // The request will proceed as unauthenticated. If the endpoint requires auth,
        // Spring Security's AuthorizationFilter will reject it and trigger the
        // JwtAuthenticationEntryPoint (401 response).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the JWT string (everything after "Bearer ")
        final String jwt = authHeader.substring(7);

        try {
            // Step 4: Parse the JWT and extract the username (subject claim).
            // JJWT validates signature AND expiration during parsing.
            // If invalid → throws JwtException → caught below → request is unauthenticated.
            final String username = jwtService.extractUsername(jwt);

            // Step 5: Only authenticate if:
            // - We successfully extracted a username
            // - There's no existing authentication in this request's SecurityContext
            //   (prevents re-authentication if another filter already handled it)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 6: Load fresh user data from the database.
                // This ensures deactivated users can't use old tokens.
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Step 7: Verify the token is valid for this user.
                // - extractUsername already validated signature + expiry
                // - isTokenValid also checks username matches (defense in depth)
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Step 8: Create an Authentication token and set it in the SecurityContext.
                    // - principal = userDetails (the UserPrincipal)
                    // - credentials = null (we don't need the password after authentication)
                    // - authorities = the user's roles (e.g., ROLE_DISPATCHER)
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Attach request details (IP address, session ID) for audit/logging
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Step 9: Store the authentication in the SecurityContext.
                    // From this point on, the user is "authenticated" for this request.
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Authenticated user: {} with role: {}",
                            username, userDetails.getAuthorities());
                }
            }
        } catch (Exception e) {
            // Token is invalid, expired, or tampered with.
            // We do NOT throw — we simply don't set authentication.
            // If the endpoint requires auth, SecurityConfig will reject it with 401.
            log.debug("JWT authentication failed: {}", e.getMessage());
        }

        // Step 10: ALWAYS continue the filter chain, regardless of auth success/failure.
        // Unauthenticated requests will be rejected by Spring Security's AuthorizationFilter
        // if they hit a protected endpoint.
        filterChain.doFilter(request, response);
    }
}
