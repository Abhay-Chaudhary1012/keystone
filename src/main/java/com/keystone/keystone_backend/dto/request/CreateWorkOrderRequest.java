package com.keystone.keystone_backend.dto.request;

import com.keystone.keystone_backend.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating a new Work Order: {@code POST /api/work-orders}.
 *
 * <p><strong>VALIDATION RULES:</strong></p>
 * <ul>
 *   <li>{@code title} — required, max 255 chars</li>
 *   <li>{@code priority} — required (LOW, MEDIUM, HIGH, CRITICAL)</li>
 *   <li>{@code customerId} — required (which customer org this WO is for)</li>
 *   <li>{@code description} — optional</li>
 *   <li>{@code siteId} — optional (can be assigned later)</li>
 *   <li>{@code notes} — optional (internal notes)</li>
 * </ul>
 *
 * <p><strong>NOT INCLUDED IN THE REQUEST:</strong></p>
 * <ul>
 *   <li>{@code code} — generated server-side (e.g., "WO-000001")</li>
 *   <li>{@code status} — defaults to OPEN on creation</li>
 *   <li>{@code assignedTechnicianId} — set during dispatch (M3)</li>
 *   <li>{@code createdById} — extracted from the JWT principal</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkOrderRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    /** Optional — the site where work will be performed. */
    private Long siteId;

    /** Optional — internal notes. */
    private String notes;
}
