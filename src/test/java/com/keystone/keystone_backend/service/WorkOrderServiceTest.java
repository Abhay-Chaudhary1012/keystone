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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WorkOrderService}.
 *
 * <p>Uses Mockito to isolate service logic from the database.
 * Tests cover: creation, validation, immutability, customer isolation,
 * site-customer ownership, lifecycle transitions, assignment, and
 * technician access control.</p>
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private WorkOrderService workOrderService;

    // Test fixtures
    private Customer testCustomer;
    private Customer otherCustomer;
    private Site testSite;
    private Site otherSite;
    private User dispatcherUser;
    private User customerUser;
    private User technicianUser;
    private User otherTechnicianUser;
    private User managerUser;
    private UserPrincipal dispatcherPrincipal;
    private UserPrincipal customerPrincipal;
    private UserPrincipal technicianPrincipal;
    private UserPrincipal otherTechnicianPrincipal;
    private UserPrincipal managerPrincipal;

    @BeforeEach
    void setUp() {
        // Customer org
        testCustomer = Customer.builder()
                .id(1L).name("Acme Corporation").code("CUST-001").build();
        otherCustomer = Customer.builder()
                .id(2L).name("Other Corp").code("CUST-002").build();

        // Sites
        testSite = Site.builder()
                .id(1L).name("Downtown Office").address("123 Main St")
                .customer(testCustomer).active(true).build();
        otherSite = Site.builder()
                .id(2L).name("Other Site").address("456 Other St")
                .customer(otherCustomer).active(true).build();

        // Users
        dispatcherUser = User.builder()
                .id(1L).username("dispatcher1").email("disp@keystone.com")
                .fullName("Diana Prince").role(Role.DISPATCHER).active(true).build();
        customerUser = User.builder()
                .id(4L).username("customer1").email("cust@acme.com")
                .fullName("Bruce Wayne").role(Role.CUSTOMER).customer(testCustomer).active(true).build();
        technicianUser = User.builder()
                .id(2L).username("technician1").email("tech@keystone.com")
                .fullName("Clark Kent").role(Role.TECHNICIAN).active(true).build();
        otherTechnicianUser = User.builder()
                .id(5L).username("technician2").email("tech2@keystone.com")
                .fullName("Barry Allen").role(Role.TECHNICIAN).active(true).build();
        managerUser = User.builder()
                .id(3L).username("manager1").email("mgr@keystone.com")
                .fullName("Amanda Waller").role(Role.MANAGER).active(true).build();

        // Principals (simulating JWT auth context)
        dispatcherPrincipal = new UserPrincipal(
                1L, "dispatcher1", "disp@keystone.com", "Diana Prince",
                "hash", Role.DISPATCHER, null, true,
                List.of(new SimpleGrantedAuthority("ROLE_DISPATCHER")));

        customerPrincipal = new UserPrincipal(
                4L, "customer1", "cust@acme.com", "Bruce Wayne",
                "hash", Role.CUSTOMER, 1L, true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        technicianPrincipal = new UserPrincipal(
                2L, "technician1", "tech@keystone.com", "Clark Kent",
                "hash", Role.TECHNICIAN, null, true,
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN")));

        otherTechnicianPrincipal = new UserPrincipal(
                5L, "technician2", "tech2@keystone.com", "Barry Allen",
                "hash", Role.TECHNICIAN, null, true,
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN")));

        managerPrincipal = new UserPrincipal(
                3L, "manager1", "mgr@keystone.com", "Amanda Waller",
                "hash", Role.MANAGER, null, true,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }


    // ==========================================
    // CREATE TESTS
    // ==========================================

    @Nested
    @DisplayName("Create Work Order")
    class CreateWorkOrder {

        @Test
        @DisplayName("should create WO with valid request")
        void createWithValidRequest() {
            CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                    .title("Fix HVAC Unit")
                    .description("Unit not cooling")
                    .priority(Priority.HIGH)
                    .customerId(1L)
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(userRepository.findById(1L)).thenReturn(Optional.of(dispatcherUser));
            when(workOrderRepository.count()).thenReturn(0L);
            when(workOrderRepository.existsByCode("WO-000001")).thenReturn(false);
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
                WorkOrder wo = invocation.getArgument(0);
                wo.setId(1L);
                wo.setCreatedAt(LocalDateTime.now());
                wo.setUpdatedAt(LocalDateTime.now());
                return wo;
            });

            WorkOrderResponse result = workOrderService.createWorkOrder(request, dispatcherPrincipal);

            assertThat(result.getCode()).isEqualTo("WO-000001");
            assertThat(result.getTitle()).isEqualTo("Fix HVAC Unit");
            assertThat(result.getStatus()).isEqualTo("OPEN");
            assertThat(result.getPriority()).isEqualTo("HIGH");
            assertThat(result.getCustomerId()).isEqualTo(1L);
            assertThat(result.getCustomerName()).isEqualTo("Acme Corporation");
            assertThat(result.getCreatedByName()).isEqualTo("Diana Prince");

            verify(workOrderRepository).save(any(WorkOrder.class));
        }

        @Test
        @DisplayName("should create WO with optional site")
        void createWithSite() {
            CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                    .title("Fix HVAC")
                    .priority(Priority.MEDIUM)
                    .customerId(1L)
                    .siteId(1L)
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(siteRepository.findById(1L)).thenReturn(Optional.of(testSite));
            when(userRepository.findById(1L)).thenReturn(Optional.of(dispatcherUser));
            when(workOrderRepository.count()).thenReturn(0L);
            when(workOrderRepository.existsByCode(anyString())).thenReturn(false);
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
                WorkOrder wo = invocation.getArgument(0);
                wo.setId(1L);
                wo.setCreatedAt(LocalDateTime.now());
                wo.setUpdatedAt(LocalDateTime.now());
                return wo;
            });

            WorkOrderResponse result = workOrderService.createWorkOrder(request, dispatcherPrincipal);

            assertThat(result.getSiteId()).isEqualTo(1L);
            assertThat(result.getSiteName()).isEqualTo("Downtown Office");
        }

        @Test
        @DisplayName("should reject when customer not found")
        void createWithInvalidCustomer() {
            CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                    .title("Fix HVAC").priority(Priority.HIGH).customerId(999L).build();

            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    workOrderService.createWorkOrder(request, dispatcherPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer");
        }

        @Test
        @DisplayName("should reject when site does not belong to customer")
        void createWithSiteMismatch() {
            CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                    .title("Fix HVAC").priority(Priority.HIGH)
                    .customerId(1L).siteId(2L).build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(siteRepository.findById(2L)).thenReturn(Optional.of(otherSite));

            assertThatThrownBy(() ->
                    workOrderService.createWorkOrder(request, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("does not belong to customer");
        }

        @Test
        @DisplayName("should generate sequential codes")
        void generateSequentialCodes() {
            CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                    .title("Fix HVAC").priority(Priority.LOW).customerId(1L).build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(userRepository.findById(1L)).thenReturn(Optional.of(dispatcherUser));
            when(workOrderRepository.count()).thenReturn(5L);
            when(workOrderRepository.existsByCode("WO-000006")).thenReturn(false);
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
                WorkOrder wo = invocation.getArgument(0);
                wo.setId(6L);
                wo.setCreatedAt(LocalDateTime.now());
                wo.setUpdatedAt(LocalDateTime.now());
                return wo;
            });

            WorkOrderResponse result = workOrderService.createWorkOrder(request, dispatcherPrincipal);
            assertThat(result.getCode()).isEqualTo("WO-000006");
        }
    }

    // ==========================================
    // READ TESTS
    // ==========================================

    @Nested
    @DisplayName("Get Work Order")
    class GetWorkOrder {

        @Test
        @DisplayName("should return WO for authorized user")
        void getExistingWorkOrder() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            WorkOrderResponse result = workOrderService.getWorkOrderById(1L, dispatcherPrincipal);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("WO-000001");
        }

        @Test
        @DisplayName("should throw 404 for non-existent WO")
        void getNonExistentWorkOrder() {
            when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    workOrderService.getWorkOrderById(999L, dispatcherPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("customer should access own org's WO")
        void customerAccessOwnWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            WorkOrderResponse result = workOrderService.getWorkOrderById(1L, customerPrincipal);
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("customer should NOT access another org's WO")
        void customerCannotAccessOtherOrgWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            wo.setCustomer(otherCustomer); // belongs to a different org

            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            // Should throw 404 (not 403) to avoid leaking existence
            assertThatThrownBy(() ->
                    workOrderService.getWorkOrderById(1L, customerPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==========================================
    // UPDATE TESTS
    // ==========================================

    @Nested
    @DisplayName("Update Work Order")
    class UpdateWorkOrder {

        @Test
        @DisplayName("should update editable fields on OPEN WO")
        void updateOpenWorkOrder() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                    .title("Updated Title")
                    .priority(Priority.CRITICAL)
                    .notes("Urgent fix needed")
                    .build();

            WorkOrderResponse result = workOrderService.updateWorkOrder(1L, request, dispatcherPrincipal);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getPriority()).isEqualTo("CRITICAL");
            assertThat(result.getNotes()).isEqualTo("Urgent fix needed");
        }

        @Test
        @DisplayName("should reject update on COMPLETED WO")
        void rejectUpdateOnCompletedWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.COMPLETED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                    .title("New Title").build();

            assertThatThrownBy(() ->
                    workOrderService.updateWorkOrder(1L, request, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("should reject update on CANCELLED WO")
        void rejectUpdateOnCancelledWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.CANCELLED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                    .title("New Title").build();

            assertThatThrownBy(() ->
                    workOrderService.updateWorkOrder(1L, request, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("should allow update on ASSIGNED WO")
        void allowUpdateOnAssignedWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ASSIGNED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                    .title("Updated").build();

            WorkOrderResponse result = workOrderService.updateWorkOrder(1L, request, dispatcherPrincipal);
            assertThat(result.getTitle()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("should validate site-customer ownership on update")
        void rejectSiteMismatchOnUpdate() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(siteRepository.findById(2L)).thenReturn(Optional.of(otherSite));

            UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                    .siteId(2L).build();

            assertThatThrownBy(() ->
                    workOrderService.updateWorkOrder(1L, request, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("does not belong to customer");
        }
    }

    // ==========================================
    // CANCEL TESTS (now uses state machine)
    // ==========================================

    @Nested
    @DisplayName("Cancel Work Order")
    class CancelWorkOrder {

        @Test
        @DisplayName("should cancel OPEN WO")
        void cancelOpenWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.cancelWorkOrder(1L, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should cancel ASSIGNED WO")
        void cancelAssignedWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ASSIGNED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.cancelWorkOrder(1L, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should cancel IN_PROGRESS WO")
        void cancelInProgressWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.IN_PROGRESS);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.cancelWorkOrder(1L, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should cancel ON_HOLD WO")
        void cancelOnHoldWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ON_HOLD);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.cancelWorkOrder(1L, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should reject cancelling already COMPLETED WO")
        void rejectCancelCompletedWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.COMPLETED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() ->
                    workOrderService.cancelWorkOrder(1L, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("should reject cancelling already CANCELLED WO")
        void rejectCancelCancelledWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.CANCELLED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() ->
                    workOrderService.cancelWorkOrder(1L, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }
    }

    // ==========================================
    // LIFECYCLE TRANSITION TESTS (M3)
    // ==========================================

    @Nested
    @DisplayName("Lifecycle Transitions")
    class LifecycleTransitions {

        // --- VALID TRANSITIONS ---

        @Test
        @DisplayName("OPEN → ASSIGNED (assign with technician)")
        void openToAssigned() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userRepository.findById(2L)).thenReturn(Optional.of(technicianUser));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(2L).build();
            WorkOrderResponse result = workOrderService.assignWorkOrder(1L, request, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("ASSIGNED");
            assertThat(result.getAssignedTechnicianId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("ASSIGNED → IN_PROGRESS (start)")
        void assignedToInProgress() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ASSIGNED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.startWorkOrder(1L, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("IN_PROGRESS → ON_HOLD (hold)")
        void inProgressToOnHold() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.IN_PROGRESS);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.holdWorkOrder(1L, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("ON_HOLD");
        }

        @Test
        @DisplayName("ON_HOLD → IN_PROGRESS (resume)")
        void onHoldToInProgress() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ON_HOLD);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.resumeWorkOrder(1L, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("IN_PROGRESS → COMPLETED (complete)")
        void inProgressToCompleted() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.IN_PROGRESS);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.completeWorkOrder(1L, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
        }


                // --- TECHNICIAN LIFECYCLE ACCESS ---

        @Test
        @DisplayName("Assigned technician can start their own work order")
        void assignedTechnicianCanStart() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ASSIGNED);
            wo.setAssignedTechnician(technicianUser);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.startWorkOrder(1L, technicianPrincipal);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Assigned technician can put their own work order on hold")
        void assignedTechnicianCanHold() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.IN_PROGRESS);
            wo.setAssignedTechnician(technicianUser);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.holdWorkOrder(1L, technicianPrincipal);

            assertThat(result.getStatus()).isEqualTo("ON_HOLD");
        }

        @Test
        @DisplayName("Assigned technician can resume their own work order")
        void assignedTechnicianCanResume() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ON_HOLD);
            wo.setAssignedTechnician(technicianUser);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.resumeWorkOrder(1L, technicianPrincipal);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Assigned technician can complete their own work order")
        void assignedTechnicianCanComplete() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.IN_PROGRESS);
            wo.setAssignedTechnician(technicianUser);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            WorkOrderResponse result = workOrderService.completeWorkOrder(1L, technicianPrincipal);

            assertThat(result.getStatus()).isEqualTo("COMPLETED");
        }

        // --- INVALID TRANSITIONS ---

        @Test
        @DisplayName("OPEN → IN_PROGRESS should be rejected (must assign first)")
        void rejectOpenToInProgress() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() ->
                    workOrderService.startWorkOrder(1L, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("OPEN → COMPLETED should be rejected (cannot skip states)")
        void rejectOpenToCompleted() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() ->
                    workOrderService.completeWorkOrder(1L, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("ASSIGNED → COMPLETED should be rejected (must start first)")
        void rejectAssignedToCompleted() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ASSIGNED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() ->
                    workOrderService.completeWorkOrder(1L, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("IN_PROGRESS → ASSIGNED should be rejected (no reverse)")
        void rejectInProgressToAssigned() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.IN_PROGRESS);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(2L).build();
            assertThatThrownBy(() ->
                    workOrderService.assignWorkOrder(1L, request, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("COMPLETED → anything should be rejected (terminal)")
        void rejectCompletedToAnything() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.COMPLETED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() ->
                    workOrderService.startWorkOrder(1L, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("CANCELLED → anything should be rejected (terminal)")
        void rejectCancelledToAnything() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.CANCELLED);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(2L).build();
            assertThatThrownBy(() ->
                    workOrderService.assignWorkOrder(1L, request, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("ON_HOLD → COMPLETED should be rejected (must resume first)")
        void rejectOnHoldToCompleted() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ON_HOLD);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() ->
                    workOrderService.completeWorkOrder(1L, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not allowed");
        }

        // --- FULL LIFECYCLE WALKTHROUGH ---

        @Test
        @DisplayName("Full lifecycle: OPEN → ASSIGNED → IN_PROGRESS → ON_HOLD → IN_PROGRESS → COMPLETED")
        void fullLifecycle() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userRepository.findById(2L)).thenReturn(Optional.of(technicianUser));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            // OPEN → ASSIGNED
            AssignWorkOrderRequest assignReq = AssignWorkOrderRequest.builder()
                    .technicianId(2L).build();
            WorkOrderResponse r1 = workOrderService.assignWorkOrder(1L, assignReq, dispatcherPrincipal);
            assertThat(r1.getStatus()).isEqualTo("ASSIGNED");

            // ASSIGNED → IN_PROGRESS
            WorkOrderResponse r2 = workOrderService.startWorkOrder(1L, dispatcherPrincipal);
            assertThat(r2.getStatus()).isEqualTo("IN_PROGRESS");

            // IN_PROGRESS → ON_HOLD
            WorkOrderResponse r3 = workOrderService.holdWorkOrder(1L, dispatcherPrincipal);
            assertThat(r3.getStatus()).isEqualTo("ON_HOLD");

            // ON_HOLD → IN_PROGRESS (resume)
            WorkOrderResponse r4 = workOrderService.resumeWorkOrder(1L, dispatcherPrincipal);
            assertThat(r4.getStatus()).isEqualTo("IN_PROGRESS");

            // IN_PROGRESS → COMPLETED
            WorkOrderResponse r5 = workOrderService.completeWorkOrder(1L, dispatcherPrincipal);
            assertThat(r5.getStatus()).isEqualTo("COMPLETED");
        }
    }

    // ==========================================
    // UPDATE DOES NOT CHANGE STATUS
    // ==========================================

    @Nested
    @DisplayName("Update does not change status")
    class UpdateDoesNotChangeStatus {

        @Test
        @DisplayName("update endpoint should not change WO status")
        void updateShouldNotChangeStatus() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            // Update title — status should remain OPEN
            UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                    .title("Updated Title")
                    .build();

            WorkOrderResponse result = workOrderService.updateWorkOrder(1L, request, dispatcherPrincipal);
            assertThat(result.getStatus()).isEqualTo("OPEN");
            assertThat(result.getTitle()).isEqualTo("Updated Title");
        }
    }

    // ==========================================
    // TECHNICIAN ASSIGNMENT TESTS (M3 Step 2)
    // ==========================================


    @Nested
    @DisplayName("Technician Assignment")
    class TechnicianAssignment {

        @Test
        @DisplayName("dispatcher assigns valid technician → OPEN → ASSIGNED")
        void dispatcherAssignsValidTechnician() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userRepository.findById(2L)).thenReturn(Optional.of(technicianUser));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(2L).build();

            WorkOrderResponse result = workOrderService.assignWorkOrder(1L, request, dispatcherPrincipal);

            assertThat(result.getStatus()).isEqualTo("ASSIGNED");
            assertThat(result.getAssignedTechnicianId()).isEqualTo(2L);
            assertThat(result.getAssignedTechnicianName()).isEqualTo("Clark Kent");
            verify(notificationService).notifyAssignment(technicianUser, wo);
        }

        @Test
        @DisplayName("manager can also assign a technician")
        void managerAssignsTechnician() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userRepository.findById(2L)).thenReturn(Optional.of(technicianUser));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(2L).build();

            WorkOrderResponse result = workOrderService.assignWorkOrder(1L, request, managerPrincipal);

            assertThat(result.getStatus()).isEqualTo("ASSIGNED");
            assertThat(result.getAssignedTechnicianId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("reject assignment of non-technician user (e.g., DISPATCHER)")
        void rejectNonTechnicianAssignment() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userRepository.findById(1L)).thenReturn(Optional.of(dispatcherUser));

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(1L).build();

            assertThatThrownBy(() ->
                    workOrderService.assignWorkOrder(1L, request, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("does not have the TECHNICIAN role");
        }

        @Test
        @DisplayName("reject assignment of non-existent user")
        void rejectNonExistentUser() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(999L).build();

            assertThatThrownBy(() ->
                    workOrderService.assignWorkOrder(1L, request, dispatcherPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }

        @Test
        @DisplayName("reject assignment of inactive technician")
        void rejectInactiveTechnician() {
            User inactiveTech = User.builder()
                    .id(10L).username("inactive_tech").email("inactive@keystone.com")
                    .fullName("Inactive Tech").role(Role.TECHNICIAN).active(false).build();

            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userRepository.findById(10L)).thenReturn(Optional.of(inactiveTech));

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(10L).build();

            assertThatThrownBy(() ->
                    workOrderService.assignWorkOrder(1L, request, dispatcherPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("notification is created on assignment")
        void notificationCreatedOnAssignment() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userRepository.findById(2L)).thenReturn(Optional.of(technicianUser));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

            AssignWorkOrderRequest request = AssignWorkOrderRequest.builder()
                    .technicianId(2L).build();

            workOrderService.assignWorkOrder(1L, request, dispatcherPrincipal);

            verify(notificationService, times(1)).notifyAssignment(technicianUser, wo);
        }
    }

    // ==========================================
    // TECHNICIAN ACCESS CONTROL (M3 Step 2)
    // ==========================================

    @Nested
    @DisplayName("Technician Access Control")
    class TechnicianAccessControl {

        @Test
        @DisplayName("technician can access their own assigned WO")
        void technicianAccessOwnAssignedWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ASSIGNED);
            wo.setAssignedTechnician(technicianUser);  // assigned to technician1
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            WorkOrderResponse result = workOrderService.getWorkOrderById(1L, technicianPrincipal);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAssignedTechnicianId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("technician cannot access unassigned WO")
        void technicianCannotAccessUnassignedWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            // assignedTechnician is null
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() ->
                    workOrderService.getWorkOrderById(1L, technicianPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("technician cannot access another technician's assigned WO")
        void technicianCannotAccessOtherTechWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ASSIGNED);
            wo.setAssignedTechnician(otherTechnicianUser);  // assigned to technician2
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            // technician1 tries to access technician2's WO → 404
            assertThatThrownBy(() ->
                    workOrderService.getWorkOrderById(1L, technicianPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("customer isolation still works after M3 changes")
        void customerIsolationStillWorks() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.OPEN);
            wo.setCustomer(otherCustomer);  // belongs to a different org
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            // customer1 (Acme) tries to access otherCustomer's WO → 404
            assertThatThrownBy(() ->
                    workOrderService.getWorkOrderById(1L, customerPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("dispatcher can still access any WO")
        void dispatcherAccessAnyWo() {
            WorkOrder wo = buildTestWorkOrder(WorkOrderStatus.ASSIGNED);
            wo.setAssignedTechnician(technicianUser);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            WorkOrderResponse result = workOrderService.getWorkOrderById(1L, dispatcherPrincipal);
            assertThat(result.getId()).isEqualTo(1L);
        }
    }

    // ==========================================
    // TEST HELPER
    // ==========================================

    private WorkOrder buildTestWorkOrder(WorkOrderStatus status) {
        return WorkOrder.builder()
                .id(1L)
                .code("WO-000001")
                .title("Test WO")
                .description("Test description")
                .status(status)
                .priority(Priority.MEDIUM)
                .customer(testCustomer)
                .createdBy(dispatcherUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
