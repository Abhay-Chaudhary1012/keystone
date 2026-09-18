package com.keystone.keystone_backend.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderPartResponse {

    private Long id;

    private Long workOrderId;
    private String workOrderCode;

    private Long partId;
    private String partName;
    private String partNumber;

    private Integer quantity;

    /**
     * Unit cost captured when the part was logged.
     * This preserves historical costing even if inventory price changes later.
     */
    private BigDecimal unitCost;

    private BigDecimal totalCost;

    private LocalDateTime createdAt;
}