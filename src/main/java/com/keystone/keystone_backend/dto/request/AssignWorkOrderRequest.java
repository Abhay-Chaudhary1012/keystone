package com.keystone.keystone_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for assigning a technician to a work order.
 *
 * <p>Used by the {@code POST /api/work-orders/{id}/assign} endpoint.
 * The dispatcher selects a technician and the server validates that the user
 * exists and has the TECHNICIAN role.</p>
 *
 * <p><strong>SECURITY:</strong> The technicianId is validated server-side.
 * The client cannot bypass authorization by providing an arbitrary user ID.</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignWorkOrderRequest {

    /**
     * ID of the technician (User with TECHNICIAN role) to assign.
     * Server validates: user exists, active, has TECHNICIAN role.
     */
    @NotNull(message = "Technician ID is required")
    private Long technicianId;
}
