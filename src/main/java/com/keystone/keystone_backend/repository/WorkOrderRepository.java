package com.keystone.keystone_backend.repository;

import com.keystone.keystone_backend.entity.WorkOrder;
import com.keystone.keystone_backend.enums.Priority;
import com.keystone.keystone_backend.enums.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link WorkOrder} entity.
 *
 * <p>Provides data access for work orders. Key use cases:</p>
 * <ul>
 *   <li>Lookup by human-readable code (e.g., "WO-000001")</li>
 *   <li>Customer data isolation — list WOs for a specific customer org</li>
 *   <li>Technician job list — WOs assigned to a specific technician</li>
 *   <li>Status/priority filtering — Kanban board, dashboard</li>
 *   <li>Paginated list queries</li>
 *   <li>Uniqueness check for generated codes</li>
 *   <li>Report summary aggregation</li>
 * </ul>
 *
 * <p><strong>PAGINATION:</strong></p>
 * <p>Methods returning {@code Page<WorkOrder>} support paginated results.
 * Spring Data automatically handles LIMIT/OFFSET and total count queries.</p>
 */
@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    /** Find a work order by its human-readable code (e.g., "WO-000001"). */
    Optional<WorkOrder> findByCode(String code);

    /** Check if a work order code already exists (uniqueness validation). */
    boolean existsByCode(String code);

    /** Find all work orders for a customer org (customer data isolation). */
    List<WorkOrder> findByCustomerId(Long customerId);

    /** Find all work orders assigned to a specific technician. */
    List<WorkOrder> findByAssignedTechnicianId(Long technicianId);

    /** Find all work orders with a specific status (Kanban board filtering). */
    List<WorkOrder> findByStatus(WorkOrderStatus status);

    /** Find work orders for a customer filtered by status. */
    List<WorkOrder> findByCustomerIdAndStatus(Long customerId, WorkOrderStatus status);

    // ==========================================
    // Paginated queries for list endpoints
    // ==========================================

    /** Paginated list of all work orders (for DISPATCHER/MANAGER). */
    Page<WorkOrder> findAll(Pageable pageable);

    /** Paginated list for a specific customer (customer data isolation). */
    Page<WorkOrder> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Filtered + paginated search for internal staff (DISPATCHER/MANAGER).
     * All filter parameters are optional — null means "don't filter on this field".
     * The title search uses case-insensitive LIKE matching.
     */
    @Query("SELECT wo FROM WorkOrder wo WHERE "
            + "(:status IS NULL OR wo.status = :status) AND "
            + "(:priority IS NULL OR wo.priority = :priority) AND "
            + "(:customerId IS NULL OR wo.customer.id = :customerId) AND "
            + "(:search IS NULL OR LOWER(wo.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkOrder> findWithFilters(
            @Param("status") WorkOrderStatus status,
            @Param("priority") Priority priority,
            @Param("customerId") Long customerId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Filtered + paginated search scoped to a specific customer (CUSTOMER role).
     * Ensures customer data isolation at the query level.
     */
    @Query("SELECT wo FROM WorkOrder wo WHERE "
            + "wo.customer.id = :customerId AND "
            + "(:status IS NULL OR wo.status = :status) AND "
            + "(:priority IS NULL OR wo.priority = :priority) AND "
            + "(:search IS NULL OR LOWER(wo.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkOrder> findByCustomerIdWithFilters(
            @Param("customerId") Long customerId,
            @Param("status") WorkOrderStatus status,
            @Param("priority") Priority priority,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Filtered + paginated search scoped to a specific assigned technician (TECHNICIAN role).
     * Ensures technicians only see their own assigned work orders.
     */
    @Query("SELECT wo FROM WorkOrder wo WHERE "
            + "wo.assignedTechnician.id = :technicianId AND "
            + "(:status IS NULL OR wo.status = :status) AND "
            + "(:priority IS NULL OR wo.priority = :priority) AND "
            + "(:search IS NULL OR LOWER(wo.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkOrder> findByAssignedTechnicianIdWithFilters(
            @Param("technicianId") Long technicianId,
            @Param("status") WorkOrderStatus status,
            @Param("priority") Priority priority,
            @Param("search") String search,
            Pageable pageable);

    // ==========================================
    // Reporting
    // ==========================================

    /**
     * Returns the number of work orders for each lifecycle status.
     *
     * <p>The result contains rows in the form:
     * [status, count].
     * The service layer maps these rows into the report response.</p>
     */
    @Query("SELECT wo.status, COUNT(wo) "
            + "FROM WorkOrder wo "
            + "GROUP BY wo.status")
    List<Object[]> countWorkOrdersByStatus();

    /** Count work orders to generate the next sequential code. */
    long count();
}