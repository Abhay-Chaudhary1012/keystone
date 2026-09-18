package com.keystone.keystone_backend.repository;

import com.keystone.keystone_backend.entity.WorkOrderPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WorkOrderPartRepository extends JpaRepository<WorkOrderPart, Long> {

    List<WorkOrderPart> findByWorkOrderId(Long workOrderId);

    @Query("SELECT COALESCE(SUM(wop.totalCost), 0) FROM WorkOrderPart wop WHERE wop.workOrder.id = :workOrderId")
    BigDecimal sumTotalCostByWorkOrderId(Long workOrderId);
}