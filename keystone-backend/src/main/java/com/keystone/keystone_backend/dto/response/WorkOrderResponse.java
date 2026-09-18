package com.keystone.keystone_backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for Work Order API responses.
 *
 * <p>Contains all work order fields plus denormalized display data from
 * related entities (customer name, site name, technician name) so the
 * frontend does not need to make separate API calls to resolve IDs.</p>
 *
 * <p><strong>WHY DENORMALIZE?</strong></p>
 * <p>When the frontend displays a work order list, it needs to show
 * "Customer: Acme Corporation" and "Technician: Tony Stark", not just
 * raw IDs. Including these in the response avoids N+1 API calls.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderResponse {

    private Long id;
    private String code;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String notes;

    // Customer info (denormalized for display)
    private Long customerId;
    private String customerName;
    private String customerCode;

    // Site info (denormalized for display, nullable)
    private Long siteId;
    private String siteName;

    // Assigned technician (denormalized, nullable until dispatched)
    private Long assignedTechnicianId;
    private String assignedTechnicianName;

    // Audit fields
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
