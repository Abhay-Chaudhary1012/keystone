package com.keystone.keystone_backend.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSummaryResponse {

    private long totalWorkOrders;

    private long openWorkOrders;
    private long assignedWorkOrders;
    private long inProgressWorkOrders;
    private long onHoldWorkOrders;
    private long completedWorkOrders;
    private long cancelledWorkOrders;

    private BigDecimal totalPartsCost;

    private long totalTechnicianMinutes;
}