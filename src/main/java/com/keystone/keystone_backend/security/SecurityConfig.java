package com.keystone.keystone_backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration for the KEYSTONE backend.
 *
 * <p><strong>ARCHITECTURE OVERVIEW:</strong></p>
 * <pre>
 * Request → CORS → CSRF(disabled) → JwtAuthenticationFilter → AuthorizationFilter
 *                                                                    │
 *                                          ┌─────────────────────────┼──────────────────┐
 *                                          │                         │                  │
 *                                    permitAll()              authenticated()      hasRole()
 *                                   /api/auth/**              /api/**             (future)
 *                                   /swagger-ui/**
 *                                   /v3/api-docs/**
 * </pre>
 *
 * <p><strong>KEY DECISIONS:</strong></p>
 * <ul>
 *   <li><strong>CSRF disabled:</strong> CSRF protection is for session-based auth
 *       (browser cookies). JWT-based APIs are immune to CSRF because the token must
 *       be explicitly included in the Authorization header — a cross-site request
 *       can't automatically attach it.</li>
 *   <li><strong>Stateless sessions:</strong> No HTTP session is created or used.
 *       Every request must carry its own JWT. This is the correct approach for
 *       REST APIs consumed by a React SPA.</li>
 *   <li><strong>Default deny:</strong> {@code anyRequest().authenticated()} means
 *       ALL endpoints require authentication unless explicitly permitted. This is
 *       secure-by-default — forgetting to add security to a new endpoint won't
 *       leave it exposed.</li>
 * </ul>
 *
 * <p><strong>@EnableWebSecurity:</strong> Enables Spring Security's web-level security.
 * Technically auto-configured by Spring Boot, but included explicitly for clarity.</p>
 *
 * <p><strong>@EnableMethodSecurity:</strong> Enables method-level authorization annotations:</p>
 * <ul>
 *   <li>{@code @PreAuthorize("hasRole('DISPATCHER')")} — check BEFORE method executes</li>
 *   <li>{@code @PostAuthorize} — check AFTER method executes (rare)</li>
 *   <li>{@code @Secured("ROLE_MANAGER")} — simpler role check</li>
 * </ul>
 * <p>We'll use these in later phases for fine-grained endpoint authorization.</p>
 *
 * <p><strong>INTERVIEW TIP:</strong> "How does Spring Security decide the order of filters?"
 * <br>→ Spring Security has a predefined filter chain order. Our {@code JwtAuthenticationFilter}
 * is registered BEFORE {@code UsernamePasswordAuthenticationFilter} via
 * {@code addFilterBefore()}. This ensures JWT validation happens before Spring's
 * default form-login filter (which we don't use).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * Defines the security filter chain — the core security configuration.
     *
     * <p>This replaces the old {@code WebSecurityConfigurerAdapter} approach
     * (deprecated in Spring Security 5.7, removed in 6.0). The modern approach
     * uses a {@code SecurityFilterChain} bean with lambda DSL.</p>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF: Disabled for stateless JWT API
                // Safe because JWT must be explicitly sent in Authorization header
                .csrf(csrf -> csrf.disable())

                // 2. CORS: Allow React frontend dev server to call this API
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Session: STATELESS — no server-side sessions
                // Each request is independently authenticated via JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Error handling: JSON error responses (not HTML)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401 handler
                        .accessDeniedHandler(customAccessDeniedHandler))         // 403 handler

                // 5. Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no JWT required
                        .requestMatchers("/api/auth/login").permitAll()

                        // Swagger/OpenAPI documentation — publicly accessible
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll()

                        // Spring Boot error endpoint
                        .requestMatchers("/error").permitAll()

                        // ALL OTHER ENDPOINTS: require authentication (secure by default)
                        .anyRequest().authenticated()
                )

                // 6. Add our JWT filter BEFORE Spring's default auth filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password encoder bean — BCrypt with default strength (cost factor 10).
     *
     * <p><strong>WHY BCrypt?</strong></p>
     * <ul>
     *   <li>Deliberately slow — resistant to brute-force attacks</li>
     *   <li>Includes salt in the hash — no rainbow table attacks</li>
     *   <li>Configurable cost factor — can increase as hardware gets faster</li>
     *   <li>Industry standard for password storage</li>
     * </ul>
     *
     * <p><strong>INTERVIEW TIP:</strong> "Why not use SHA-256 for passwords?"
     * <br>→ SHA-256 is a fast hash — an attacker can compute billions per second.
     * BCrypt is intentionally slow (configurable via cost factor). A cost of 10
     * means 2^10 = 1024 rounds, making each hash take ~100ms. This makes
     * brute-force impractical while being acceptable for normal login.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration for React frontend development.
     *
     * <p>Allows requests from common React dev server ports (3000 for CRA, 5173 for Vite).
     * In production, this should be restricted to the actual frontend domain.</p>
     *
     * <p><strong>WHY CORS IS NEEDED:</strong></p>
     * <p>Browsers enforce the Same-Origin Policy — a React app at localhost:3000
     * cannot call an API at localhost:8080 unless the API explicitly allows it via
     * CORS headers. Without this config, the browser blocks the request.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",   // React Create React App default
                "http://localhost:5173"    // Vite default
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
