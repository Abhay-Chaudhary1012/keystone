package com.keystone.keystone_backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeEntryResponse {

    private Long id;

    private Long workOrderId;
    private String workOrderCode;

    private Long technicianId;
    private String technicianName;

    private Integer minutes;

    private String notes;

    private LocalDateTime createdAt;
}