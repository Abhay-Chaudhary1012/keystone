package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.dto.response.WorkOrderPartResponse;
import com.keystone.keystone_backend.entity.Customer;
import com.keystone.keystone_backend.entity.Part;
import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.entity.WorkOrder;
import com.keystone.keystone_backend.entity.WorkOrderPart;
import com.keystone.keystone_backend.enums.Priority;
import com.keystone.keystone_backend.enums.Role;
import com.keystone.keystone_backend.enums.WorkOrderStatus;
import com.keystone.keystone_backend.exception.BusinessRuleException;
import com.keystone.keystone_backend.repository.PartRepository;
import com.keystone.keystone_backend.repository.WorkOrderPartRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderPartRepository workOrderPartRepository;

    @InjectMocks
    private PartService partService;

    private Customer testCustomer;
    private User technicianUser;
    private UserPrincipal technicianPrincipal;
    private WorkOrder assignedWorkOrder;
    private Part testPart;

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

        testPart = Part.builder()
                .id(1L)
                .name("Air Filter")
                .partNumber("PART-001")
                .unitCost(new BigDecimal("25.00"))
                .stockQuantity(10)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("should log part and deduct inventory stock")
    void logPartSuccessfully() {

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(assignedWorkOrder));

        when(partRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(testPart));

        when(partRepository.save(any(Part.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(workOrderPartRepository.save(any(WorkOrderPart.class)))
                .thenAnswer(invocation -> {
                    WorkOrderPart wop = invocation.getArgument(0);
                    wop.setId(1L);
                    return wop;
                });

        WorkOrderPartResponse result = partService.logPart(
                1L,
                1L,
                2,
                technicianPrincipal
        );

        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getUnitCost())
                .isEqualByComparingTo("25.00");
        assertThat(result.getTotalCost())
                .isEqualByComparingTo("50.00");

        assertThat(testPart.getStockQuantity()).isEqualTo(8);

        verify(partRepository).findByIdForUpdate(1L);
        verify(partRepository).save(testPart);
        verify(workOrderPartRepository).save(any(WorkOrderPart.class));
    }

    @Test
    @DisplayName("should reject zero quantity")
    void rejectZeroQuantity() {

        assertThatThrownBy(() ->
                partService.logPart(
                        1L,
                        1L,
                        0,
                        technicianPrincipal
                ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(workOrderRepository);
        verifyNoInteractions(partRepository);
        verifyNoInteractions(workOrderPartRepository);
    }

    @Test
    @DisplayName("should reject negative quantity")
    void rejectNegativeQuantity() {

        assertThatThrownBy(() ->
                partService.logPart(
                        1L,
                        1L,
                        -2,
                        technicianPrincipal
                ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(workOrderRepository);
        verifyNoInteractions(partRepository);
        verifyNoInteractions(workOrderPartRepository);
    }

    @Test
    @DisplayName("should reject insufficient inventory stock")
    void rejectInsufficientStock() {

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(assignedWorkOrder));

        testPart.setStockQuantity(1);

        when(partRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(testPart));

        assertThatThrownBy(() ->
                partService.logPart(
                        1L,
                        1L,
                        2,
                        technicianPrincipal
                ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");

        assertThat(testPart.getStockQuantity()).isEqualTo(1);

        verify(partRepository, never()).save(any(Part.class));
        verify(workOrderPartRepository, never()).save(any(WorkOrderPart.class));
    }

    @Test
    @DisplayName("should reject inactive part")
    void rejectInactivePart() {

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(assignedWorkOrder));

        testPart.setActive(false);

        when(partRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(testPart));

        assertThatThrownBy(() ->
                partService.logPart(
                        1L,
                        1L,
                        1,
                        technicianPrincipal
                ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");

        verify(partRepository, never()).save(any(Part.class));
        verify(workOrderPartRepository, never()).save(any(WorkOrderPart.class));
    }
}