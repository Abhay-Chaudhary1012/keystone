package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.dto.response.ReportSummaryResponse;
import com.keystone.keystone_backend.enums.WorkOrderStatus;
import com.keystone.keystone_backend.repository.TimeEntryRepository;
import com.keystone.keystone_backend.repository.WorkOrderPartRepository;
import com.keystone.keystone_backend.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReportService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderPartRepository workOrderPartRepository;
    private final TimeEntryRepository timeEntryRepository;

    public ReportService(
            WorkOrderRepository workOrderRepository,
            WorkOrderPartRepository workOrderPartRepository,
            TimeEntryRepository timeEntryRepository) {

        this.workOrderRepository = workOrderRepository;
        this.workOrderPartRepository = workOrderPartRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary() {

        List<Object[]> statusCounts =
                workOrderRepository.countWorkOrdersByStatus();

        long open = 0;
        long assigned = 0;
        long inProgress = 0;
        long onHold = 0;
        long completed = 0;
        long cancelled = 0;

        for (Object[] row : statusCounts) {

            WorkOrderStatus status = (WorkOrderStatus) row[0];
            long count = ((Number) row[1]).longValue();

            switch (status) {
                case OPEN -> open = count;
                case ASSIGNED -> assigned = count;
                case IN_PROGRESS -> inProgress = count;
                case ON_HOLD -> onHold = count;
                case COMPLETED -> completed = count;
                case CANCELLED -> cancelled = count;
            }
        }

        BigDecimal totalPartsCost =
                workOrderPartRepository
                        .findAll()
                        .stream()
                        .map(workOrderPart -> workOrderPart.getTotalCost())
                        .filter(cost -> cost != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTechnicianMinutes =
                timeEntryRepository
                        .findAll()
                        .stream()
                        .mapToLong(timeEntry ->
                                timeEntry.getMinutes() != null
                                        ? timeEntry.getMinutes()
                                        : 0L)
                        .sum();

        return ReportSummaryResponse.builder()
                .totalWorkOrders(workOrderRepository.count())
                .openWorkOrders(open)
                .assignedWorkOrders(assigned)
                .inProgressWorkOrders(inProgress)
                .onHoldWorkOrders(onHold)
                .completedWorkOrders(completed)
                .cancelledWorkOrders(cancelled)
                .totalPartsCost(totalPartsCost)
                .totalTechnicianMinutes(totalTechnicianMinutes)
                .build();
    }
}