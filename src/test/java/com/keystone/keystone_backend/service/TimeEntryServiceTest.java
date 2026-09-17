package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.dto.response.TimeEntryResponse;
import com.keystone.keystone_backend.entity.Customer;
import com.keystone.keystone_backend.entity.TimeEntry;
import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.entity.WorkOrder;
import com.keystone.keystone_backend.enums.Priority;
import com.keystone.keystone_backend.enums.Role;
import com.keystone.keystone_backend.enums.WorkOrderStatus;
import com.keystone.keystone_backend.exception.BusinessRuleException;
import com.keystone.keystone_backend.exception.ResourceNotFoundException;
import com.keystone.keystone_backend.repository.TimeEntryRepository;
import com.keystone.keystone_backend.repository.WorkOrderRepository;
import com.keystone.keystone_backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private TimeEntryService timeEntryService;

    private Customer testCustomer;
    private User technicianUser;
    private User otherTechnicianUser;
    private UserPrincipal technicianPrincipal;
    private UserPrincipal otherTechnicianPrincipal;
    private WorkOrder assignedWorkOrder;

    @BeforeEach
    void setUp() {

        testCustomer = Customer.builder()
                .id(1L)
                .name("Acme Corporation")
                .code("CUST-001")
                .build();

        technicianUser = User.builder()
                .id(2L)
                .username("technician1")
                .email("tech@keystone.com")
                .fullName("Clark Kent")
                .role(Role.TECHNICIAN)
                .active(true)
                .build();

        otherTechnicianUser = User.builder()
                .id(5L)
                .username("technician2")
                .email("tech2@keystone.com")
                .fullName("Barry Allen")
                .role(Role.TECHNICIAN)
                .active(true)
                .build();

        technicianPrincipal = new UserPrincipal(
                2L,
                "technician1",
                "tech@keystone.com",
                "Clark Kent",
                "hash",
                Role.TECHNICIAN,
                null,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))
        );

        otherTechnicianPrincipal = new UserPrincipal(
                5L,
                "technician2",
                "tech2@keystone.com",
                "Barry Allen",
                "hash",
                Role.TECHNICIAN,
                null,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))
        );

        assignedWorkOrder = WorkOrder.builder()
                .id(1L)
                .code("WO-000001")
                .title("HVAC Repair")
                .description("Repair HVAC unit")
                .status(WorkOrderStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .customer(testCustomer)
                .assignedTechnician(technicianUser)
                .createdBy(technicianUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should log valid technician time")
    void logTimeSuccessfully() {

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(assignedWorkOrder));

        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(invocation -> {
                    TimeEntry entry = invocation.getArgument(0);
                    entry.setId(1L);
                    entry.setCreatedAt(LocalDateTime.now());
                    return entry;
                });

        TimeEntryResponse result = timeEntryService.logTime(
                1L,
                90,
                "HVAC diagnosis and repair",
                technicianPrincipal
        );

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getWorkOrderId()).isEqualTo(1L);
        assertThat(result.getWorkOrderCode()).isEqualTo("WO-000001");
        assertThat(result.getTechnicianId()).isEqualTo(2L);
        assertThat(result.getTechnicianName()).isEqualTo("Clark Kent");
        assertThat(result.getMinutes()).isEqualTo(90);
        assertThat(result.getNotes())
                .isEqualTo("HVAC diagnosis and repair");

        verify(workOrderRepository).findById(1L);
        verify(timeEntryRepository).save(any(TimeEntry.class));
    }

    @Test
    @DisplayName("should allow time entry without notes")
    void logTimeWithoutNotes() {

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(assignedWorkOrder));

        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TimeEntryResponse result = timeEntryService.logTime(
                1L,
                30,
                null,
                technicianPrincipal
        );

        assertThat(result.getMinutes()).isEqualTo(30);
        assertThat(result.getNotes()).isNull();

        verify(timeEntryRepository).save(any(TimeEntry.class));
    }

    @Test
    @DisplayName("should reject zero minutes")
    void rejectZeroMinutes() {

        assertThatThrownBy(() ->
                timeEntryService.logTime(
                        1L,
                        0,
                        "Invalid entry",
                        technicianPrincipal
                ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(workOrderRepository);
        verifyNoInteractions(timeEntryRepository);
    }

    @Test
    @DisplayName("should reject negative minutes")
    void rejectNegativeMinutes() {

        assertThatThrownBy(() ->
                timeEntryService.logTime(
                        1L,
                        -30,
                        "Invalid entry",
                        technicianPrincipal
                ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(workOrderRepository);
        verifyNoInteractions(timeEntryRepository);
    }

    @Test
    @DisplayName("technician cannot log time on another technician's work order")
    void rejectOtherTechnicianWorkOrder() {

        assignedWorkOrder.setAssignedTechnician(otherTechnicianUser);

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(assignedWorkOrder));

        assertThatThrownBy(() ->
                timeEntryService.logTime(
                        1L,
                        60,
                        "Unauthorized entry",
                        technicianPrincipal
                ))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
    }

    @Test
    @DisplayName("should reject time logging when work order does not exist")
    void rejectMissingWorkOrder() {

        when(workOrderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                timeEntryService.logTime(
                        999L,
                        60,
                        "Test entry",
                        technicianPrincipal
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Work Order");

        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
    }

    @Test
    @DisplayName("should reject non-technician principal")
    void rejectNonTechnician() {

        UserPrincipal customerPrincipal = new UserPrincipal(
                4L,
                "customer1",
                "cust@acme.com",
                "Bruce Wayne",
                "hash",
                Role.CUSTOMER,
                1L,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(assignedWorkOrder));

        assertThatThrownBy(() ->
                timeEntryService.logTime(
                        1L,
                        60,
                        "Unauthorized entry",
                        customerPrincipal
                ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only technicians");

        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
    }
}