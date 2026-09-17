package com.keystone.keystone_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles authentication failures for protected endpoints (HTTP 401).
 *
 * <p><strong>WHEN IS THIS CALLED?</strong></p>
 * <p>When a request reaches a protected endpoint WITHOUT valid authentication:</p>
 * <ul>
 *   <li>No Authorization header at all</li>
 *   <li>Authorization header without "Bearer " prefix</li>
 *   <li>Expired JWT token</li>
 *   <li>Tampered/invalid JWT token</li>
 * </ul>
 * <p>In all these cases, the {@code JwtAuthenticationFilter} does NOT set the
 * SecurityContext, and Spring Security's authorization check fails. The
 * {@code ExceptionTranslationFilter} then calls this entry point.</p>
 *
 * <p><strong>WHY NOT INJECT ObjectMapper?</strong></p>
 * <p>Spring Boot 4.x auto-configures an ObjectMapper from {@code tools.jackson.core}
 * (Jackson 3.x), but our compile-time import would reference
 * {@code com.fasterxml.jackson.databind.ObjectMapper} (Jackson 2.x). These are
 * different classes, so Spring cannot inject one into the other. We write JSON
 * directly to avoid this version mismatch.</p>
 *
 * <p><strong>INTERVIEW TIP:</strong> "What is the difference between AuthenticationEntryPoint
 * and AccessDeniedHandler?"
 * <br>→ AuthenticationEntryPoint handles 401 (WHO ARE YOU? — user is not authenticated).
 * AccessDeniedHandler handles 403 (I KNOW WHO YOU ARE, BUT YOU CAN'T DO THIS —
 * user is authenticated but lacks the required role/authority).</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String json = String.format(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required to access this resource\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now().format(FORMATTER),
                escapeJson(request.getRequestURI())
        );

        response.getWriter().write(json);
        response.getWriter().flush();
    }

    /** Minimal JSON escaping for the path string to prevent injection. */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
