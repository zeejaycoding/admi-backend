package com.powercity.power_city_platform.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.Report;
import com.powercity.power_city_platform.entity.ReportReceipt;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.ReportService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Report Management", description = "APIs for managing coordinator reports")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('COORDINATOR')")
    @Operation(summary = "Create coordinator report", description = "Submit a new report with expenses and remittance receipt files")
    public ResponseEntity<ApiResponse<Report>> createReport(
            @RequestParam("date") String date,
            @RequestParam("country") String country,
            @RequestParam(value = "nationalLeader", required = false) String nationalLeader,
            @RequestParam("campus") String campus,
            @RequestParam("coordinator") String coordinator,
            @RequestParam(value = "zonalLeader", required = false) String zonalLeader,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "partnership", defaultValue = "0.0") Double partnership,
            @RequestParam(value = "papaHonour", defaultValue = "0.0") Double papaHonour,
            @RequestParam(value = "offerings", defaultValue = "0.0") Double offerings,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "expenses", required = false) String expensesJson,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        try {
            User currentUser = userService.getCurrentUser();
            LocalDate parsedDate = LocalDate.parse(date);
            List<Map<String, Object>> rawExpenses = null;

            if (expensesJson != null && !expensesJson.trim().isEmpty()) {
                rawExpenses = objectMapper.readValue(expensesJson, new TypeReference<List<Map<String, Object>>>() {});
            }

            Report createdReport = reportService.createReport(
                    parsedDate, country, nationalLeader, campus, coordinator, zonalLeader, summary,
                    partnership, papaHonour, offerings, currency, rawExpenses, files, currentUser
            );

            return ResponseEntity.ok(ApiResponse.success("Report submitted successfully", createdReport));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to submit report: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('COORDINATOR')")
    @Operation(summary = "Get all reports", description = "Retrieve list of reports (Admins see all; Coordinators see their own submissions)")
    public ResponseEntity<ApiResponse<List<Report>>> getAllReports() {
        User currentUser = userService.getCurrentUser();
        List<Report> reports = reportService.getAllReports(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved successfully", reports));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('COORDINATOR')")
    @Operation(summary = "Get report by ID", description = "Retrieve details of a specific report")
    public ResponseEntity<ApiResponse<Report>> getReportById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Report report = reportService.getReportById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Report details retrieved successfully", report));
    }

    @PutMapping("/{id}/status")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update report status", description = "Approve or update status of a report")
    public ResponseEntity<ApiResponse<Report>> updateReportStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        User currentUser = userService.getCurrentUser();
        Report updatedReport = reportService.updateReportStatus(id, status, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Report status updated to " + status, updatedReport));
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('COORDINATOR')")
    @Operation(summary = "Update report", description = "Edit a report's name and info section")
    public ResponseEntity<ApiResponse<Report>> updateReport(
            @PathVariable Long id,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String country,
            @RequestParam(value = "nationalLeader", required = false) String nationalLeader,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) String coordinator,
            @RequestParam(value = "zonalLeader", required = false) String zonalLeader,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String currency) {
        User currentUser = userService.getCurrentUser();

        LocalDate parsedDate = null;
        if (date != null && !date.trim().isEmpty()) {
            parsedDate = LocalDate.parse(date);
        }

        Report updatedReport = reportService.updateReport(
                id, parsedDate, country, nationalLeader, campus, coordinator, zonalLeader, summary, currency, currentUser
        );
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", updatedReport));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('COORDINATOR')")
    @Operation(summary = "Delete report", description = "Delete a report")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        reportService.deleteReport(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Report deleted successfully", null));
    }

    @GetMapping("/{id}/receipts/{receiptId}/download")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('COORDINATOR')")
    @Operation(summary = "Download report receipt", description = "Stream a remittance receipt file for a report")
    public ResponseEntity<?> downloadReceipt(@PathVariable Long id, @PathVariable Long receiptId) {
        try {
            User currentUser = userService.getCurrentUser();
            Report report = reportService.getReportById(id, currentUser);
            ReportReceipt receipt = report.getReceipts().stream()
                    .filter(r -> r.getId().equals(receiptId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Receipt not found"));

            return reportService.streamReceipt(receipt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to download receipt: " + e.getMessage()));
        }
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('COORDINATOR')")
    @Operation(summary = "Get report analytics", description = "Retrieve reporting summary statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportAnalytics() {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> analytics = reportService.getReportAnalytics(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }
}
