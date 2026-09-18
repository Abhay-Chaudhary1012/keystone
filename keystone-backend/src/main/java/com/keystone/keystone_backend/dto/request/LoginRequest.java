package com.keystone.keystone_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for the login endpoint: {@code POST /api/auth/login}.
 *
 * <p><strong>WHY A DTO INSTEAD OF TAKING RAW PARAMETERS?</strong></p>
 * <ul>
 *   <li>DTOs enforce a contract — the frontend knows exactly what to send</li>
 *   <li>Jakarta Bean Validation annotations ({@code @NotBlank}) provide automatic
 *       input validation before the request even reaches the service layer</li>
 *   <li>DTOs decouple the API contract from internal domain objects</li>
 * </ul>
 *
 * <p><strong>Annotation: {@code @NotBlank}</strong></p>
 * <p>Validates that the field is not null, not empty, and not just whitespace.
 * Different from {@code @NotNull} (allows empty string) and {@code @NotEmpty}
 * (allows whitespace-only strings). {@code @NotBlank} is the strictest.</p>
 *
 * <p><strong>INTERVIEW TIP:</strong> "How does validation work in Spring Boot?"
 * <br>→ When a controller parameter is annotated with {@code @Valid} or
 * {@code @Validated}, Spring invokes the Jakarta Bean Validation provider
 * (Hibernate Validator) to check all constraint annotations. If validation
 * fails, a {@code MethodArgumentNotValidException} is thrown BEFORE the
 * controller method body executes.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
