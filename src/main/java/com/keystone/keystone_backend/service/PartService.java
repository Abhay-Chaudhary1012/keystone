package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.dto.response.WorkOrderPartResponse;
import com.keystone.keystone_backend.entity.Part;
import com.keystone.keystone_backend.entity.WorkOrder;
import com.keystone.keystone_backend.entity.WorkOrderPart;
import com.keystone.keystone_backend.exception.BusinessRuleException;
import com.keystone.keystone_backend.exception.ResourceNotFoundException;
import com.keystone.keystone_backend.repository.PartRepository;
import com.keystone.keystone_backend.repository.WorkOrderPartRepository;
import com.keystone.keystone_backend.repository.WorkOrderRepository;
import com.keystone.keystone_backend.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderPartRepository workOrderPartRepository;

    public PartService(
            PartRepository partRepository,
            WorkOrderRepository workOrderRepository,
            WorkOrderPartRepository workOrderPartRepository) {
        this.partRepository = partRepository;
        this.workOrderRepository = workOrderRepository;
        this.workOrderPartRepository = workOrderPartRepository;
    }

    /**
     * Logs a part against a work order and deducts the part from inventory.
     *
     * <p>The entire operation runs inside one transaction so that if any
     * operation fails, the inventory deduction and work-order part record
     * are rolled back together.</p>
     */
    @Transactional
    public WorkOrderPartResponse logPart(
            Long workOrderId,
            Long partId,
            Integer quantity,
            UserPrincipal principal) {

        if (quantity == null || quantity <= 0) {
            throw new BusinessRuleException("Part quantity must be greater than zero");
        }

        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Work Order", "id", workOrderId));

        validateTechnicianAccess(workOrder, principal);

        Part part = partRepository.findByIdForUpdate(partId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Part", "id", partId));

        if (!Boolean.TRUE.equals(part.getActive())) {
            throw new BusinessRuleException("Part is inactive");
        }

        if (part.getStockQuantity() < quantity) {
            throw new BusinessRuleException(
                    "Insufficient stock for part " + part.getPartNumber()
                            + ". Available: " + part.getStockQuantity()
                            + ", requested: " + quantity);
        }

        BigDecimal totalCost = part.getUnitCost()
                .multiply(BigDecimal.valueOf(quantity));

        part.setStockQuantity(part.getStockQuantity() - quantity);
        partRepository.save(part);

        WorkOrderPart workOrderPart = WorkOrderPart.builder()
                .workOrder(workOrder)
                .part(part)
                .quantity(quantity)
                .unitCost(part.getUnitCost())
                .totalCost(totalCost)
                .build();

        WorkOrderPart savedPart =
                workOrderPartRepository.save(workOrderPart);

        return mapToResponse(savedPart);
    }

    private WorkOrderPartResponse mapToResponse(
            WorkOrderPart workOrderPart) {

        WorkOrder workOrder = workOrderPart.getWorkOrder();
        Part part = workOrderPart.getPart();

        return WorkOrderPartResponse.builder()
                .id(workOrderPart.getId())
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .partId(part.getId())
                .partName(part.getName())
                .partNumber(part.getPartNumber())
                .quantity(workOrderPart.getQuantity())
                .unitCost(workOrderPart.getUnitCost())
                .totalCost(workOrderPart.getTotalCost())
                .createdAt(workOrderPart.getCreatedAt())
                .build();
    }

    private void validateTechnicianAccess(
            WorkOrder workOrder,
            UserPrincipal principal) {

        if (principal == null || principal.getRole() == null
                || !"TECHNICIAN".equals(principal.getRole().name())) {
            throw new BusinessRuleException(
                    "Only technicians can log parts");
        }

        if (workOrder.getAssignedTechnician() == null
                || !workOrder.getAssignedTechnician().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException(
                    "Work Order", "id", workOrder.getId());
        }
    }
}