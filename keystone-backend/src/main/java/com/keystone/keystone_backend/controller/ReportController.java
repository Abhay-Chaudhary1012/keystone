package com.keystone.keystone_backend.controller;

import com.keystone.keystone_backend.dto.response.ReportSummaryResponse;
import com.keystone.keystone_backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Work order and execution summary reports")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    @Operation(
            summary = "Get report summary",
            description = "Returns overall work order status counts, total parts cost, "
                    + "and total technician time logged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report summary returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ReportSummaryResponse> getSummary() {

        ReportSummaryResponse response = reportService.getSummary();

        return ResponseEntity.ok(response);
    }
}