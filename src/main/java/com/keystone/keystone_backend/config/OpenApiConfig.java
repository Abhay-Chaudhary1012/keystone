package com.keystone.keystone_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the KEYSTONE API.
 *
 * <p><strong>WHAT THIS DOES:</strong></p>
 * <p>Configures the Swagger UI available at {@code /swagger-ui.html} (or {@code /swagger-ui/index.html}).
 * This provides:</p>
 * <ul>
 *   <li>Interactive API documentation — try endpoints directly from the browser</li>
 *   <li>Request/response schema visualization</li>
 *   <li>JWT authentication support — click "Authorize", paste your token, and all
 *       subsequent requests include the Bearer token automatically</li>
 * </ul>
 *
 * <p><strong>JWT IN SWAGGER:</strong></p>
 * <p>The {@code SecurityScheme} of type HTTP with scheme "bearer" adds an "Authorize"
 * button to the Swagger UI. After pasting a JWT token, Swagger includes
 * {@code Authorization: Bearer <token>} in all API requests.</p>
 *
 * <p><strong>INTERVIEW TIP:</strong> "What is the difference between Swagger and OpenAPI?"
 * <br>→ OpenAPI is the SPECIFICATION (the standard format for describing REST APIs).
 * Swagger is the TOOLING (Swagger UI for visualization, Swagger Codegen for
 * client generation). SpringDoc generates an OpenAPI 3.0 spec from your Spring
 * controllers and serves the Swagger UI to visualize it.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI keystoneOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KEYSTONE API")
                        .description("Field Service Management Platform — REST API documentation. "
                                + "Use the Authorize button to set your JWT token for testing protected endpoints.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("KEYSTONE Team")))
                // Add a global security requirement — shows the lock icon on all endpoints
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                // Define the security scheme
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .bearerFormat("JWT")
                                        .scheme("bearer")
                                        .description("Enter your JWT token obtained from POST /api/auth/login")));
    }
}
