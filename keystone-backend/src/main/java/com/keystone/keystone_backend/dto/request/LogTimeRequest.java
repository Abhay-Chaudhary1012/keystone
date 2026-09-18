package com.keystone.keystone_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogTimeRequest {

    @NotNull(message = "Minutes are required")
    @Positive(message = "Minutes must be greater than zero")
    private Integer minutes;

    private String notes;
}