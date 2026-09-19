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
 * <p>
 * Request flow:
 * CORS -> CSRF(disabled) -> JwtAuthenticationFilter -> AuthorizationFilter
 * </p>
 *
 * <p>
 * The application uses stateless JWT authentication. All endpoints require
 * authentication by default except explicitly permitted public endpoints.
 * </p>
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
     * Defines the Spring Security filter chain.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 1. CSRF: Disabled for stateless JWT API
                .csrf(csrf -> csrf.disable())

                // 2. CORS: Allow configured frontend origins
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Session: STATELESS — no server-side sessions
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Error handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))

                // 5. Authorization rules
                .authorizeHttpRequests(auth -> auth

                        // Public authentication endpoint
                        .requestMatchers("/api/auth/login").permitAll()

                        // Swagger/OpenAPI documentation
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll()

                        // Spring Boot error endpoint
                        .requestMatchers("/error").permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // 6. JWT filter before Spring's default authentication filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Password encoder using BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration for local development and the deployed Render frontend.
     *
     * <p>
     * Local development:
     * - http://localhost:3000
     * - http://localhost:5173
     *
     * Production:
     * - https://keystone-1-hpi4.onrender.com
     * </p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://keystone-1-hpi4.onrender.com"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}