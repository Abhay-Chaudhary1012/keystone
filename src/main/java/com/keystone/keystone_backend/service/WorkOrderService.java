package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.dto.request.AssignWorkOrderRequest;
import com.keystone.keystone_backend.dto.request.CreateWorkOrderRequest;
import com.keystone.keystone_backend.dto.request.UpdateWorkOrderRequest;
import com.keystone.keystone_backend.dto.response.WorkOrderResponse;
import com.keystone.keystone_backend.entity.Customer;
import com.keystone.keystone_backend.entity.Site;
import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.entity.WorkOrder;
import com.keystone.keystone_backend.enums.Priority;
import com.keystone.keystone_backend.enums.Role;
import com.keystone.keystone_backend.enums.WorkOrderStatus;
import com.keystone.keystone_backend.exception.BusinessRuleException;
import com.keystone.keystone_backend.exception.ResourceNotFoundException;
import com.keystone.keystone_backend.repository.CustomerRepository;
import com.keystone.keystone_backend.repository.SiteRepository;
import com.keystone.keystone_backend.repository.UserRepository;
import com.keystone.keystone_backend.repository.WorkOrderRepository;
import com.keystone.keystone_backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Service handling Work Order business logic.
 *
 * <p><strong>RESPONSIBILITIES:</strong></p>
 * <ul>
 *   <li>Create work orders with server-generated unique codes</li>
 *   <li>Validate customer/site relationships (site must belong to the WO's customer)</li>
 *   <li>Enforce immutability for COMPLETED/CANCELLED work orders</li>
 *   <li>Enforce explicit lifecycle state transitions (M3)</li>
 *   <li>Map entities to DTOs</li>
 *   <li>Apply customer data isolation for CUSTOMER-role users</li>
 * </ul>
 *
 * <p><strong>LIFECYCLE STATE MACHINE (M3):</strong></p>
 * <pre>
 *   OPEN → ASSIGNED            (dispatcher assigns technician — M3 Step 2)
 *   OPEN → CANCELLED           (cancel)
 *   ASSIGNED → IN_PROGRESS     (technician starts work — M3 Step 2)
 *   ASSIGNED → CANCELLED       (cancel)
 *   IN_PROGRESS → ON_HOLD      (technician puts on hold — M3 Step 2)
 *   IN_PROGRESS → COMPLETED    (technician completes — M3 Step 2)
 *   IN_PROGRESS → CANCELLED    (cancel)
 *   ON_HOLD → IN_PROGRESS      (technician resumes — M3 Step 2)
 *   ON_HOLD → CANCELLED        (cancel)
 * </pre>
 * <p>COMPLETED and CANCELLED are terminal — no transitions out.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Valid state transitions for the Work Order lifecycle.
     *
     * <p>Each entry maps a current status to the set of statuses it can transition to.
     * Terminal states (COMPLETED, CANCELLED) have empty sets — no transitions out.</p>
     *
     * <p><strong>WHY A MAP?</strong> Centralizes all transition rules in one place.
     * Adding/removing transitions only requires changing this map, not hunting through
     * scattered if/else blocks. Also makes the rules easy to test and audit.</p>
     */
    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(WorkOrderStatus.class);
        VALID_TRANSITIONS.put(WorkOrderStatus.OPEN,
                Set.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(WorkOrderStatus.ASSIGNED,
                Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(WorkOrderStatus.IN_PROGRESS,
                Set.of(WorkOrderStatus.ON_HOLD, WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(WorkOrderStatus.ON_HOLD,
                Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(WorkOrderStatus.COMPLETED, Set.of());  // terminal
        VALID_TRANSITIONS.put(WorkOrderStatus.CANCELLED, Set.of());  // terminal
    }


    // ==========================================
    // CREATE
    // ==========================================

    /**
     * Creates a new work order.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Validate customer exists</li>
     *   <li>Validate site exists and belongs to the customer (if provided)</li>
     *   <li>Generate unique human-readable code (WO-000001)</li>
     *   <li>Set status to OPEN, createdBy from JWT principal</li>
     *   <li>Save and return DTO</li>
     * </ol>
     *
     * @param request  The create request DTO
     * @param principal The authenticated user creating this WO
     * @return The created work order as a response DTO
     */
    @Transactional
    public WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request,
                                             UserPrincipal principal) {

        // 1. Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", "id", request.getCustomerId()));

        // 2. Validate site (if provided) exists and belongs to this customer
        Site site = null;
        if (request.getSiteId() != null) {
            site = validateSiteBelongsToCustomer(request.getSiteId(), customer);
        }

        // 3. Load the creating user entity (for the FK relationship)
        var createdBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", principal.getId()));

        // 4. Generate unique code
        String code = generateNextCode();

        // 5. Build and save entity
        WorkOrder workOrder = WorkOrder.builder()
                .code(code)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(WorkOrderStatus.OPEN)
                .priority(request.getPriority())
                .customer(customer)
                .site(site)
                .createdBy(createdBy)
                .notes(request.getNotes())
                .build();

        WorkOrder saved = workOrderRepository.save(workOrder);

        log.info("Work order {} created by user {} for customer {}",
                saved.getCode(), principal.getUsername(), customer.getCode());

        return mapToResponse(saved);
    }

    // ==========================================
    // READ
    // ==========================================

    /**
     * Gets a work order by ID, enforcing customer data isolation.
     *
     * <p>CUSTOMER-role users can only access WOs belonging to their own organization.
     * DISPATCHER/MANAGER/TECHNICIAN can access any WO.</p>
     *
     * @param id        The work order database ID
     * @param principal The authenticated user
     * @return The work order as a response DTO
     */
    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrderById(Long id, UserPrincipal principal) {
        WorkOrder workOrder = findWorkOrderOrThrow(id);
        enforceAccessControl(workOrder, principal);
        return mapToResponse(workOrder);
    }

    /**
     * Lists work orders with optional filters and pagination.
     *
     * <p>CUSTOMER-role users are automatically scoped to their own organization.
     * Internal staff (DISPATCHER/MANAGER) see all work orders.</p>
     *
     * @param status    Optional status filter
     * @param priority  Optional priority filter
     * @param search    Optional title search (case-insensitive contains)
     * @param principal The authenticated user
     * @param pageable  Pagination parameters (page, size, sort)
     * @return Paginated list of work order response DTOs
     */
    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> listWorkOrders(WorkOrderStatus status,
                                                   Priority priority,
                                                   String search,
                                                   UserPrincipal principal,
                                                   Pageable pageable) {

        // Normalize empty search to null (so the @Query treats it as "no filter")
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<WorkOrder> page;

        if (principal.getCustomerId() != null) {
            // CUSTOMER role: always scoped to their own organization
            page = workOrderRepository.findByCustomerIdWithFilters(
                    principal.getCustomerId(), status, priority,
                    normalizedSearch, pageable);
        } else if (principal.getRole() == Role.TECHNICIAN) {
            // TECHNICIAN role: only see their own assigned work orders
            page = workOrderRepository.findByAssignedTechnicianIdWithFilters(
                    principal.getId(), status, priority,
                    normalizedSearch, pageable);
        } else {
            // Internal staff (DISPATCHER/MANAGER): see all, with optional filters
            page = workOrderRepository.findWithFilters(
                    status, priority, null, normalizedSearch, pageable);
        }

        return page.map(this::mapToResponse);
    }


    // ==========================================
    // UPDATE
    // ==========================================

    /**
     * Updates a work order's editable fields.
     *
     * <p><strong>IMMUTABILITY RULE:</strong> COMPLETED and CANCELLED work orders
     * cannot be modified. This is enforced server-side.</p>
     *
     * <p><strong>NOT UPDATABLE:</strong> code, status, customerId, createdBy,
     * assignedTechnician. Status changes require explicit lifecycle operations.</p>
     *
     * @param id        The work order database ID
     * @param request   The update request DTO (all fields optional)
     * @param principal The authenticated user
     * @return The updated work order as a response DTO
     */
    @Transactional
    public WorkOrderResponse updateWorkOrder(Long id,
                                             UpdateWorkOrderRequest request,
                                             UserPrincipal principal) {

        WorkOrder workOrder = findWorkOrderOrThrow(id);
        enforceAccessControl(workOrder, principal);

        // Immutability check: COMPLETED/CANCELLED cannot be edited
        if (isTerminalStatus(workOrder.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot modify work order " + workOrder.getCode()
                    + " — status is " + workOrder.getStatus()
                    + " (terminal state)");
        }

        // Apply only provided (non-null) fields — partial update pattern
        if (request.getTitle() != null) {
            workOrder.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            workOrder.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            workOrder.setPriority(request.getPriority());
        }
        if (request.getNotes() != null) {
            workOrder.setNotes(request.getNotes());
        }

        // Site update: validate the new site belongs to the same customer
        if (request.getSiteId() != null) {
            Site site = validateSiteBelongsToCustomer(
                    request.getSiteId(), workOrder.getCustomer());
            workOrder.setSite(site);
        }

        WorkOrder saved = workOrderRepository.save(workOrder);

        log.info("Work order {} updated by user {}", saved.getCode(), principal.getUsername());

        return mapToResponse(saved);
    }

    // ==========================================
    // LIFECYCLE TRANSITIONS (M3)
    // ==========================================

    /**
     * Assigns a technician to a work order: OPEN → ASSIGNED.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Validate work order exists and is in OPEN state</li>
     *   <li>Validate the specified user exists, is active, and has TECHNICIAN role</li>
     *   <li>Set the assigned technician on the work order</li>
     *   <li>Transition status from OPEN to ASSIGNED</li>
     *   <li>Create an in-app notification for the technician</li>
     * </ol>
     *
     * @param id        The work order database ID
     * @param request   Contains the technicianId to assign
     * @param principal The authenticated user (DISPATCHER or MANAGER)
     * @return The updated work order as a response DTO
     */
    @Transactional
    public WorkOrderResponse assignWorkOrder(Long id,
                                             AssignWorkOrderRequest request,
                                             UserPrincipal principal) {
        WorkOrder workOrder = findWorkOrderOrThrow(id);
        enforceAccessControl(workOrder, principal);

        // Validate transition
        validateTransition(workOrder, WorkOrderStatus.ASSIGNED);

        // Validate technician
        User technician = validateTechnician(request.getTechnicianId());

        // Set technician and transition status
        workOrder.setAssignedTechnician(technician);
        WorkOrderStatus previousStatus = workOrder.getStatus();
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        WorkOrder saved = workOrderRepository.save(workOrder);

        // Create in-app notification for the technician
        notificationService.notifyAssignment(technician, saved);

        log.info("Work order {} assigned to technician {} by {} ({} → ASSIGNED)",
                saved.getCode(), technician.getUsername(),
                principal.getUsername(), previousStatus);

        return mapToResponse(saved);
    }


    /**
     * Starts work on a work order: ASSIGNED → IN_PROGRESS.
     *
     * <p>In the full workflow (M3 Step 2), this would be triggered by the
     * assigned technician. For now, DISPATCHER/MANAGER can trigger it.</p>
     *
     * @param id        The work order database ID
     * @param principal The authenticated user
     * @return The updated work order as a response DTO
     */
    @Transactional
    public WorkOrderResponse startWorkOrder(Long id, UserPrincipal principal) {
        return performTransition(id, WorkOrderStatus.IN_PROGRESS, "started", principal);
    }

    /**
     * Puts a work order on hold: IN_PROGRESS → ON_HOLD.
     *
     * <p>Used when work cannot continue (waiting for parts, access issues, etc.).</p>
     *
     * @param id        The work order database ID
     * @param principal The authenticated user
     * @return The updated work order as a response DTO
     */
    @Transactional
    public WorkOrderResponse holdWorkOrder(Long id, UserPrincipal principal) {
        return performTransition(id, WorkOrderStatus.ON_HOLD, "put on hold", principal);
    }

    /**
     * Resumes a work order: ON_HOLD → IN_PROGRESS.
     *
     * @param id        The work order database ID
     * @param principal The authenticated user
     * @return The updated work order as a response DTO
     */
    @Transactional
    public WorkOrderResponse resumeWorkOrder(Long id, UserPrincipal principal) {
        return performTransition(id, WorkOrderStatus.IN_PROGRESS, "resumed", principal);
    }

    /**
     * Completes a work order: IN_PROGRESS → COMPLETED.
     *
     * <p>COMPLETED is a terminal state — no further transitions allowed.</p>
     *
     * @param id        The work order database ID
     * @param principal The authenticated user
     * @return The updated work order as a response DTO
     */
    @Transactional
    public WorkOrderResponse completeWorkOrder(Long id, UserPrincipal principal) {
        return performTransition(id, WorkOrderStatus.COMPLETED, "completed", principal);
    }

    /**
     * Cancels a work order: any non-terminal state → CANCELLED.
     *
     * <p>CANCELLED is a terminal state — no further transitions allowed.</p>
     *
     * @param id        The work order database ID
     * @param principal The authenticated user
     * @return The cancelled work order as a response DTO
     */
    @Transactional
    public WorkOrderResponse cancelWorkOrder(Long id, UserPrincipal principal) {
        return performTransition(id, WorkOrderStatus.CANCELLED, "cancelled", principal);
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    /**
     * Performs a state transition with validation.
     *
     * <p>This is the single point of enforcement for all lifecycle transitions.
     * It validates the transition against the {@link #VALID_TRANSITIONS} map,
     * applies the new status, saves, and logs the change.</p>
     *
     * @param id           The work order database ID
     * @param targetStatus The desired new status
     * @param actionVerb   Human-readable verb for logging (e.g., "assigned", "completed")
     * @param principal    The authenticated user
     * @return The updated work order as a response DTO
     * @throws ResourceNotFoundException if the work order doesn't exist
     * @throws BusinessRuleException if the transition is not valid
     */
    private WorkOrderResponse performTransition(Long id,
                                                WorkOrderStatus targetStatus,
                                                String actionVerb,
                                                UserPrincipal principal) {
        WorkOrder workOrder = findWorkOrderOrThrow(id);
        enforceAccessControl(workOrder, principal);

        validateTransition(workOrder, targetStatus);

        WorkOrderStatus previousStatus = workOrder.getStatus();
        workOrder.setStatus(targetStatus);
        WorkOrder saved = workOrderRepository.save(workOrder);

        log.info("Work order {} {} by user {} ({} → {})",
                saved.getCode(), actionVerb, principal.getUsername(),
                previousStatus, targetStatus);

        return mapToResponse(saved);
    }

    /**
     * Validates that a status transition is allowed by the state machine.
     *
     * @throws BusinessRuleException if the transition is invalid
     */
    private void validateTransition(WorkOrder workOrder, WorkOrderStatus targetStatus) {
        WorkOrderStatus currentStatus = workOrder.getStatus();
        Set<WorkOrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowed.contains(targetStatus)) {
            throw new BusinessRuleException(
                    "Invalid status transition for work order " + workOrder.getCode()
                    + ": " + currentStatus + " → " + targetStatus + " is not allowed");
        }
    }


    /**
     * Generates the next sequential work order code.
     *
     * <p>Format: WO-000001, WO-000002, etc.</p>
     *
     * <p>Uses the current count of work orders + 1. The UNIQUE constraint
     * on the 'code' column prevents duplicates if two requests race.
     * In that case, the second save will throw a constraint violation,
     * which is acceptable for development. A production system could use
     * a database sequence instead.</p>
     */
    private String generateNextCode() {
        long nextNumber = workOrderRepository.count() + 1;
        String code = String.format("WO-%06d", nextNumber);

        // Safety: if code already exists (e.g., after deletes), increment until unique
        while (workOrderRepository.existsByCode(code)) {
            nextNumber++;
            code = String.format("WO-%06d", nextNumber);
        }

        return code;
    }

    /** Finds a work order by ID or throws 404. */
    private WorkOrder findWorkOrderOrThrow(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work Order", "id", id));
    }

    /**
     * Validates that a site exists and belongs to the specified customer.
     *
     * @throws ResourceNotFoundException if site does not exist
     * @throws BusinessRuleException if site belongs to a different customer
     */
    private Site validateSiteBelongsToCustomer(Long siteId, Customer customer) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site", "id", siteId));

        if (!site.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessRuleException(
                    "Site " + siteId + " does not belong to customer " + customer.getCode());
        }

        return site;
    }

    /**
     * Enforces data access control based on user role.
     *
     * <p><strong>CUSTOMER</strong> users can only access work orders belonging to their
     * own organization. Returns 404 (not 403) to avoid leaking existence.</p>
     *
     * <p><strong>TECHNICIAN</strong> users can only access work orders assigned to them.
     * Returns 404 (not 403) to avoid leaking existence.</p>
     *
     * <p><strong>DISPATCHER/MANAGER</strong> can access any work order.</p>
     */
    private void enforceAccessControl(WorkOrder workOrder, UserPrincipal principal) {
        // Customer isolation: CUSTOMER role → scoped to own org
        if (principal.getCustomerId() != null
                && !workOrder.getCustomer().getId().equals(principal.getCustomerId())) {
            throw new ResourceNotFoundException("Work Order", "id", workOrder.getId());
        }

        // Technician isolation: TECHNICIAN role → scoped to assigned WOs
        if (principal.getRole() == Role.TECHNICIAN) {
            if (workOrder.getAssignedTechnician() == null
                    || !workOrder.getAssignedTechnician().getId().equals(principal.getId())) {
                throw new ResourceNotFoundException("Work Order", "id", workOrder.getId());
            }
        }
    }

    /**
     * Validates that a user exists, is active, and has the TECHNICIAN role.
     *
     * @param technicianId The user ID to validate
     * @return The validated User entity
     * @throws ResourceNotFoundException if user does not exist
     * @throws BusinessRuleException if user is not active or not a TECHNICIAN
     */
    private User validateTechnician(Long technicianId) {
        User user = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", technicianId));

        if (user.getRole() != Role.TECHNICIAN) {
            throw new BusinessRuleException(
                    "User " + user.getUsername() + " does not have the TECHNICIAN role");
        }

        if (!user.getActive()) {
            throw new BusinessRuleException(
                    "Technician " + user.getUsername() + " is not active");
        }

        return user;
    }

    /** Checks if a status is a terminal (immutable) state. */
    private boolean isTerminalStatus(WorkOrderStatus status) {
        return status == WorkOrderStatus.COMPLETED || status == WorkOrderStatus.CANCELLED;
    }

    /**
     * Maps a WorkOrder entity to a WorkOrderResponse DTO.
     *
     * <p>Denormalizes related entity data (customer name, site name,
     * technician name) so the frontend doesn't need separate API calls.</p>
     */
    private WorkOrderResponse mapToResponse(WorkOrder wo) {
        WorkOrderResponse.WorkOrderResponseBuilder builder = WorkOrderResponse.builder()
                .id(wo.getId())
                .code(wo.getCode())
                .title(wo.getTitle())
                .description(wo.getDescription())
                .status(wo.getStatus().name())
                .priority(wo.getPriority().name())
                .notes(wo.getNotes())
                .customerId(wo.getCustomer().getId())
                .customerName(wo.getCustomer().getName())
                .customerCode(wo.getCustomer().getCode())
                .createdById(wo.getCreatedBy().getId())
                .createdByName(wo.getCreatedBy().getFullName())
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt());

        // Site (nullable)
        if (wo.getSite() != null) {
            builder.siteId(wo.getSite().getId())
                   .siteName(wo.getSite().getName());
        }

        // Assigned technician (nullable — set during dispatch, M3)
        if (wo.getAssignedTechnician() != null) {
            builder.assignedTechnicianId(wo.getAssignedTechnician().getId())
                   .assignedTechnicianName(wo.getAssignedTechnician().getFullName());
        }

        return builder.build();
    }
}
