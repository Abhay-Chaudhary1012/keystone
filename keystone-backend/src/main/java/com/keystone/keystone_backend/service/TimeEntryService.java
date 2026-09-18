package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.dto.response.TimeEntryResponse;
import com.keystone.keystone_backend.entity.TimeEntry;
import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.entity.WorkOrder;
import com.keystone.keystone_backend.exception.BusinessRuleException;
import com.keystone.keystone_backend.exception.ResourceNotFoundException;
import com.keystone.keystone_backend.repository.TimeEntryRepository;
import com.keystone.keystone_backend.repository.WorkOrderRepository;
import com.keystone.keystone_backend.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final WorkOrderRepository workOrderRepository;

    public TimeEntryService(
            TimeEntryRepository timeEntryRepository,
            WorkOrderRepository workOrderRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @Transactional
    public TimeEntryResponse logTime(
            Long workOrderId,
            Integer minutes,
            String notes,
            UserPrincipal principal) {

        if (minutes == null || minutes <= 0) {
            throw new BusinessRuleException(
                    "Logged time must be greater than zero minutes");
        }

        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Work Order", "id", workOrderId));

        validateTechnicianAccess(workOrder, principal);

        User technician = User.builder()
                .id(principal.getId())
                .fullName(principal.getFullName())
                .build();

        TimeEntry timeEntry = TimeEntry.builder()
                .workOrder(workOrder)
                .user(technician)
                .minutes(minutes)
                .notes(notes)
                .build();

        TimeEntry savedEntry = timeEntryRepository.save(timeEntry);

        return mapToResponse(savedEntry);
    }

    private TimeEntryResponse mapToResponse(TimeEntry timeEntry) {

        WorkOrder workOrder = timeEntry.getWorkOrder();
        User technician = timeEntry.getUser();

        return TimeEntryResponse.builder()
                .id(timeEntry.getId())
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .technicianId(technician.getId())
                .technicianName(technician.getFullName())
                .minutes(timeEntry.getMinutes())
                .notes(timeEntry.getNotes())
                .createdAt(timeEntry.getCreatedAt())
                .build();
    }

    private void validateTechnicianAccess(
            WorkOrder workOrder,
            UserPrincipal principal) {

        if (principal == null
                || principal.getRole() == null
                || !"TECHNICIAN".equals(principal.getRole().name())) {

            throw new BusinessRuleException(
                    "Only technicians can log time");
        }

        if (workOrder.getAssignedTechnician() == null
                || !workOrder.getAssignedTechnician()
                        .getId()
                        .equals(principal.getId())) {

            throw new ResourceNotFoundException(
                    "Work Order", "id", workOrder.getId());
        }
    }
}