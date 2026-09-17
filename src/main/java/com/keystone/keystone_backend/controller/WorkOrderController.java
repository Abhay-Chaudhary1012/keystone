package com.keystone.keystone_backend.controller;

import com.keystone.keystone_backend.dto.request.AssignWorkOrderRequest;
import com.keystone.keystone_backend.dto.request.CreateWorkOrderRequest;
import com.keystone.keystone_backend.dto.request.UpdateWorkOrderRequest;
import com.keystone.keystone_backend.dto.response.WorkOrderResponse;
import com.keystone.keystone_backend.enums.Priority;
import com.keystone.keystone_backend.enums.WorkOrderStatus;
import com.keystone.keystone_backend.security.UserPrincipal;
import com.keystone.keystone_backend.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Work Order CRUD operations.
 *
 * <p><strong>ENDPOINTS:</strong></p>
 * <ul>
 *   <li>{@code POST   /api/work-orders}       — Create a work order</li>
 *   <li>{@code GET    /api/work-orders}        — List work orders (paginated, filtered)</li>
 *   <li>{@code GET    /api/work-orders/{id}}   — Get a work order by ID</li>
 *   <li>{@code PUT    /api/work-orders/{id}}   — Update a work order</li>
 *   <li>{@code POST   /api/work-orders/{id}/cancel} — Cancel a work order</li>
 * </ul>
 *
 * <p><strong>AUTHORIZATION:</strong></p>
 * <ul>
 *   <li><strong>CREATE:</strong> DISPATCHER, MANAGER, CUSTOMER</li>
 *   <li><strong>READ (list/get):</strong> ALL authenticated roles</li>
 *   <li><strong>UPDATE:</strong> DISPATCHER, MANAGER</li>
 *   <li><strong>CANCEL:</strong> DISPATCHER, MANAGER</li>
 * </ul>
 *
 * <p>Customer data isolation is enforced at the service layer — CUSTOMER-role users
 * can only see/create work orders for their own organization.</p>
 *
 * <p><strong>THIN CONTROLLER:</strong> No business logic here. All validation,
 * data access, and business rules are in {@code WorkOrderService}.</p>
 */
@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Tag(name = "Work Orders", description = "Work Order CRUD operations")
@SecurityRequirement(name = "Bearer Authentication")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    // ==========================================
    // CREATE
    // ==========================================

    /**
     * Creates a new work order.
     *
     * <p>Accessible by DISPATCHER, MANAGER, and CUSTOMER roles.
     * The createdBy field is set from the JWT principal — never from the client.</p>
     *
     * <p>CUSTOMER-role users can only create WOs for their own organization
     * (enforced by the service layer).</p>
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Create work order",
            description = "Creates a new work order. Status defaults to OPEN. Code is generated server-side.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Work order created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Customer or Site not found"),
            @ApiResponse(responseCode = "409", description = "Site does not belong to customer")
    })
    public ResponseEntity<WorkOrderResponse> createWorkOrder(
            @Valid @RequestBody CreateWorkOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.createWorkOrder(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // READ
    // ==========================================

    /**
     * Gets a single work order by ID.
     *
     * <p>All authenticated roles can access this endpoint.
     * Customer data isolation is enforced at the service layer.</p>
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get work order by ID",
            description = "Returns a single work order. CUSTOMER users can only access their own org's WOs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Work order not found or not accessible")
    })
    public ResponseEntity<WorkOrderResponse> getWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.getWorkOrderById(id, principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists work orders with optional filters and pagination.
     *
     * <p>All authenticated roles can access this endpoint.
     * CUSTOMER users are automatically scoped to their own organization.</p>
     *
     * <p>Default pagination: page 0, size 20, sorted by createdAt descending.</p>
     */
    @GetMapping
    @Operation(summary = "List work orders",
            description = "Returns a paginated list of work orders with optional filters. "
                    + "CUSTOMER users see only their own org's WOs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order list returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Page<WorkOrderResponse>> listWorkOrders(
            @Parameter(description = "Filter by status") @RequestParam(required = false) WorkOrderStatus status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) Priority priority,
            @Parameter(description = "Search by title (case-insensitive)") @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<WorkOrderResponse> page = workOrderService.listWorkOrders(
                status, priority, search, principal, pageable);
        return ResponseEntity.ok(page);
    }

    // ==========================================
    // UPDATE
    // ==========================================

    /**
     * Updates a work order's editable fields.
     *
     * <p>Only DISPATCHER and MANAGER can update work orders.
     * COMPLETED and CANCELLED work orders cannot be modified.</p>
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    @Operation(summary = "Update work order",
            description = "Updates editable fields of a work order. Cannot modify COMPLETED/CANCELLED WOs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Work order not found"),
            @ApiResponse(responseCode = "409", description = "Work order is in terminal state or site mismatch")
    })
    public ResponseEntity<WorkOrderResponse> updateWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.updateWorkOrder(id, request, principal);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // CANCEL
    // ==========================================

    /**
     * Cancels a work order.
     *
     * <p>Only DISPATCHER and MANAGER can cancel work orders.
     * Already COMPLETED or CANCELLED work orders cannot be cancelled again.</p>
     *
     * <p>Uses POST (not DELETE) because cancellation is a state change, not removal.
     * The work order record is preserved for audit/history purposes.</p>
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    @Operation(summary = "Cancel work order",
            description = "Sets the work order status to CANCELLED. Cannot cancel already terminal WOs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order cancelled"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Work order not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<WorkOrderResponse> cancelWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.cancelWorkOrder(id, principal);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // LIFECYCLE TRANSITIONS (M3)
    // ==========================================

    /**
     * Assigns a technician to a work order: OPEN → ASSIGNED.
     *
     * <p>Only DISPATCHER and MANAGER can assign work orders.
     * The request body must contain a valid technicianId (user with TECHNICIAN role).</p>
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    @Operation(summary = "Assign work order to technician",
            description = "Assigns a technician and transitions work order from OPEN to ASSIGNED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order assigned"),
            @ApiResponse(responseCode = "400", description = "Validation error (missing technicianId)"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Work order or technician not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition or user is not a technician")
    })
    public ResponseEntity<WorkOrderResponse> assignWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody AssignWorkOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.assignWorkOrder(id, request, principal);
        return ResponseEntity.ok(response);
    }


    /**
     * Starts work on a work order: ASSIGNED → IN_PROGRESS.
     *
     * <p>Only DISPATCHER and MANAGER can trigger this for now.
     * Technician self-service start will be added in M3 Step 2.</p>
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    @Operation(summary = "Start work order",
            description = "Transitions work order from ASSIGNED to IN_PROGRESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order started"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Work order not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<WorkOrderResponse> startWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.startWorkOrder(id, principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Puts a work order on hold: IN_PROGRESS → ON_HOLD.
     *
     * <p>Used when work cannot continue (waiting for parts, access issues, etc.).</p>
     */
    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    @Operation(summary = "Hold work order",
            description = "Transitions work order from IN_PROGRESS to ON_HOLD.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order on hold"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Work order not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<WorkOrderResponse> holdWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.holdWorkOrder(id, principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Resumes a work order: ON_HOLD → IN_PROGRESS.
     */
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    @Operation(summary = "Resume work order",
            description = "Transitions work order from ON_HOLD back to IN_PROGRESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order resumed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Work order not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<WorkOrderResponse> resumeWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.resumeWorkOrder(id, principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Completes a work order: IN_PROGRESS → COMPLETED.
     *
     * <p>COMPLETED is a terminal state — no further transitions allowed.</p>
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    @Operation(summary = "Complete work order",
            description = "Transitions work order from IN_PROGRESS to COMPLETED (terminal state).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work order completed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Work order not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<WorkOrderResponse> completeWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderResponse response = workOrderService.completeWorkOrder(id, principal);
        return ResponseEntity.ok(response);
    }
}

