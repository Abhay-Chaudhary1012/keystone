package com.keystone.keystone_backend.controller;

import com.keystone.keystone_backend.dto.request.LogPartRequest;
import com.keystone.keystone_backend.dto.request.LogTimeRequest;
import com.keystone.keystone_backend.dto.response.TimeEntryResponse;
import com.keystone.keystone_backend.dto.response.WorkOrderPartResponse;
import com.keystone.keystone_backend.security.UserPrincipal;
import com.keystone.keystone_backend.service.PartService;
import com.keystone.keystone_backend.service.TimeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Tag(name = "Work Order Execution", description = "Parts and time logging for work orders")
@SecurityRequirement(name = "Bearer Authentication")
public class WorkOrderExecutionController {

    private final PartService partService;
    private final TimeEntryService timeEntryService;

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(
            summary = "Log a part",
            description = "Logs a part against an assigned work order and deducts inventory stock."
    )
    public ResponseEntity<WorkOrderPartResponse> logPart(
            @PathVariable Long id,
            @Valid @RequestBody LogPartRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkOrderPartResponse result = partService.logPart(
                id,
                request.getPartId(),
                request.getQuantity(),
                principal
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/time")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(
            summary = "Log technician time",
            description = "Logs technician time against an assigned work order."
    )
    public ResponseEntity<TimeEntryResponse> logTime(
            @PathVariable Long id,
            @Valid @RequestBody LogTimeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TimeEntryResponse result = timeEntryService.logTime(
                id,
                request.getMinutes(),
                request.getNotes(),
                principal
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}