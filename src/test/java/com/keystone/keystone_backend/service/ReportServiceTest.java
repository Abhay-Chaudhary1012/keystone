package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.dto.response.ReportSummaryResponse;
import com.keystone.keystone_backend.entity.TimeEntry;
import com.keystone.keystone_backend.entity.WorkOrderPart;
import com.keystone.keystone_backend.enums.WorkOrderStatus;
import com.keystone.keystone_backend.repository.TimeEntryRepository;
import com.keystone.keystone_backend.repository.WorkOrderPartRepository;
import com.keystone.keystone_backend.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderPartRepository workOrderPartRepository;

    @Mock
    private TimeEntryRepository timeEntryRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                workOrderRepository,
                workOrderPartRepository,
                timeEntryRepository
        );
    }

    @Test
    void shouldBuildReportSummaryFromRepositoryData() {

        when(workOrderRepository.count()).thenReturn(10L);

        when(workOrderRepository.countWorkOrdersByStatus())
                .thenReturn(Arrays.<Object[]>asList(                        new Object[]{WorkOrderStatus.OPEN, 2L},
                        new Object[]{WorkOrderStatus.ASSIGNED, 1L},
                        new Object[]{WorkOrderStatus.IN_PROGRESS, 2L},
                        new Object[]{WorkOrderStatus.ON_HOLD, 1L},
                        new Object[]{WorkOrderStatus.COMPLETED, 3L},
                        new Object[]{WorkOrderStatus.CANCELLED, 1L}
                ));

        when(workOrderPartRepository.findAll())
                .thenReturn(Arrays.asList(
                        mockWorkOrderPart(new BigDecimal("100.00")),
                        mockWorkOrderPart(new BigDecimal("250.50")),
                        mockWorkOrderPart(new BigDecimal("49.50"))
                ));

        when(timeEntryRepository.findAll())
                .thenReturn(Arrays.asList(
                        mockTimeEntry(60),
                        mockTimeEntry(120),
                        mockTimeEntry(30)
                ));

        ReportSummaryResponse response = reportService.getSummary();

        assertEquals(10L, response.getTotalWorkOrders());

        assertEquals(2L, response.getOpenWorkOrders());
        assertEquals(1L, response.getAssignedWorkOrders());
        assertEquals(2L, response.getInProgressWorkOrders());
        assertEquals(1L, response.getOnHoldWorkOrders());
        assertEquals(3L, response.getCompletedWorkOrders());
        assertEquals(1L, response.getCancelledWorkOrders());

        assertEquals(
                new BigDecimal("400.00"),
                response.getTotalPartsCost()
        );

        assertEquals(
                210L,
                response.getTotalTechnicianMinutes()
        );
    }

    @Test
    void shouldDefaultMissingStatusesToZero() {

        when(workOrderRepository.count()).thenReturn(3L);

        when(workOrderRepository.countWorkOrdersByStatus())
                .thenReturn(Arrays.<Object[]>asList(                        new Object[]{WorkOrderStatus.OPEN, 2L},
                        new Object[]{WorkOrderStatus.COMPLETED, 1L}
                ));

        when(workOrderPartRepository.findAll())
                .thenReturn(Collections.emptyList());

        when(timeEntryRepository.findAll())
                .thenReturn(Collections.emptyList());

        ReportSummaryResponse response = reportService.getSummary();

        assertEquals(3L, response.getTotalWorkOrders());

        assertEquals(2L, response.getOpenWorkOrders());
        assertEquals(0L, response.getAssignedWorkOrders());
        assertEquals(0L, response.getInProgressWorkOrders());
        assertEquals(0L, response.getOnHoldWorkOrders());
        assertEquals(1L, response.getCompletedWorkOrders());
        assertEquals(0L, response.getCancelledWorkOrders());

        assertEquals(
                BigDecimal.ZERO,
                response.getTotalPartsCost()
        );

        assertEquals(
                0L,
                response.getTotalTechnicianMinutes()
        );
    }

    @Test
    void shouldHandleNullPartCostsAndNullTimeMinutes() {

        when(workOrderRepository.count()).thenReturn(2L);

        when(workOrderRepository.countWorkOrdersByStatus())
                .thenReturn(Arrays.<Object[]>asList(                        new Object[]{WorkOrderStatus.OPEN, 2L}
                ));

        when(workOrderPartRepository.findAll())
                .thenReturn(Arrays.asList(
                        mockWorkOrderPart(new BigDecimal("100.00")),
                        mockWorkOrderPart(null)
                ));

        when(timeEntryRepository.findAll())
                .thenReturn(Arrays.asList(
                        mockTimeEntry(90),
                        mockTimeEntry(null)
                ));

        ReportSummaryResponse response = reportService.getSummary();

        assertEquals(
                new BigDecimal("100.00"),
                response.getTotalPartsCost()
        );

        assertEquals(
                90L,
                response.getTotalTechnicianMinutes()
        );
    }

    @Test
    void shouldReturnZeroTotalsWhenNoExecutionDataExists() {

        when(workOrderRepository.count()).thenReturn(0L);

        when(workOrderRepository.countWorkOrdersByStatus())
                .thenReturn(Collections.emptyList());

        when(workOrderPartRepository.findAll())
                .thenReturn(Collections.emptyList());

        when(timeEntryRepository.findAll())
                .thenReturn(Collections.emptyList());

        ReportSummaryResponse response = reportService.getSummary();

        assertEquals(0L, response.getTotalWorkOrders());
        assertEquals(0L, response.getOpenWorkOrders());
        assertEquals(0L, response.getAssignedWorkOrders());
        assertEquals(0L, response.getInProgressWorkOrders());
        assertEquals(0L, response.getOnHoldWorkOrders());
        assertEquals(0L, response.getCompletedWorkOrders());
        assertEquals(0L, response.getCancelledWorkOrders());

        assertEquals(
                BigDecimal.ZERO,
                response.getTotalPartsCost()
        );

        assertEquals(
                0L,
                response.getTotalTechnicianMinutes()
        );
    }

   private WorkOrderPart mockWorkOrderPart(BigDecimal totalCost) {

    WorkOrderPart workOrderPart = new WorkOrderPart();

    workOrderPart.setTotalCost(totalCost);

    return workOrderPart;
}

    private TimeEntry mockTimeEntry(Integer minutes) {

    TimeEntry timeEntry = new TimeEntry();

    timeEntry.setMinutes(minutes);

    return timeEntry;
}
}
