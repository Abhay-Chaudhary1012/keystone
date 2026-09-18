package com.keystone.keystone_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles authorization failures for authenticated users (HTTP 403 Forbidden).
 *
 * <p><strong>WHEN IS THIS CALLED?</strong></p>
 * <p>When an AUTHENTICATED user tries to access an endpoint they don't have
 * the required ROLE for:</p>
 * <ul>
 *   <li>A CUSTOMER trying to access a DISPATCHER-only endpoint</li>
 *   <li>A TECHNICIAN trying to access a MANAGER-only dashboard</li>
 * </ul>
 *
 * <p><strong>401 vs 403:</strong></p>
 * <ul>
 *   <li>401 Unauthorized = "Who are you? Show me your credentials."</li>
 *   <li>403 Forbidden = "I know who you are, but you're not allowed to do this."</li>
 * </ul>
 *
 * <p>Like {@link JwtAuthenticationEntryPoint}, this writes JSON directly to the
 * response stream (no ObjectMapper injection) to avoid Jackson 2.x/3.x class conflicts
 * in Spring Boot 4.x.</p>
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String json = String.format(
                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"You do not have permission to access this resource\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
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
