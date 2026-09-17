package com.keystone.keystone_backend.dto.request;

import com.keystone.keystone_backend.enums.Priority;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for updating an existing Work Order: {@code PUT /api/work-orders/{id}}.
 *
 * <p>All fields are optional — only provided fields are updated.
 * The service layer enforces immutability for COMPLETED/CANCELLED work orders.</p>
 *
 * <p><strong>NOT UPDATABLE VIA THIS DTO:</strong></p>
 * <ul>
 *   <li>{@code code} — immutable once generated</li>
 *   <li>{@code status} — changed only via explicit lifecycle operations (assign, start, complete, etc.)</li>
 *   <li>{@code customerId} — cannot change the owning organization</li>
 *   <li>{@code createdById} — immutable audit field</li>
 *   <li>{@code assignedTechnicianId} — changed via dispatch operations (M3)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkOrderRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    private Priority priority;

    /** Optional — update the site where work will be performed. */
    private Long siteId;

    /** Optional — update internal notes. */
    private String notes;
}
