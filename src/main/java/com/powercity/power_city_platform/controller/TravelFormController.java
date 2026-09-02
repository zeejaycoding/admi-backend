package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.travel.TravelFormCreateRequest;
import com.powercity.power_city_platform.dto.request.travel.TravelFormStatusRequest;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.dto.response.travel.PowerPortalDashboardResponse;
import com.powercity.power_city_platform.dto.response.travel.TravelFormResponse;
import com.powercity.power_city_platform.service.TravelFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/travel-forms")
@Tag(name = "Travel Form Management", description = "APIs for managing travelling form submissions")
@RequiredArgsConstructor
public class TravelFormController {

    private final TravelFormService travelFormService;

    @PostMapping
    @Operation(summary = "Submit a travelling form", description = "Create a new travelling form submission")
    public ResponseEntity<ApiResponse<TravelFormResponse>> createTravelForm(
            @Valid @RequestBody TravelFormCreateRequest request) {
        try {
            TravelFormResponse response = travelFormService.createTravelForm(request);
            return ResponseEntity.ok(ApiResponse.success("Travel form submitted successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to submit travel form: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all travel forms", description = "Admins see all; regular users see only their own submissions")
    public ResponseEntity<ApiResponse<List<TravelFormResponse>>> getAllTravelForms() {
        List<TravelFormResponse> forms = travelFormService.getAllTravelForms();
        return ResponseEntity.ok(ApiResponse.success("Travel forms retrieved successfully", forms));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get travel form by ID", description = "Retrieve details of a specific travel form")
    public ResponseEntity<ApiResponse<TravelFormResponse>> getTravelFormById(@PathVariable Long id) {
        try {
            TravelFormResponse form = travelFormService.getTravelFormById(id);
            return ResponseEntity.ok(ApiResponse.success("Travel form retrieved successfully", form));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve travel form: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    @Operation(summary = "Update travel form status", description = "Approve or reject a travel form submission")
    public ResponseEntity<ApiResponse<TravelFormResponse>> updateTravelFormStatus(
            @PathVariable Long id,
            @Valid @RequestBody TravelFormStatusRequest request) {
        try {
            TravelFormResponse form = travelFormService.updateStatus(id, request.getStatus(), request.getRejectionReason());
            return ResponseEntity.ok(ApiResponse.success("Travel form " + request.getStatus().toLowerCase() + " successfully", form));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    @Operation(summary = "Delete travel form", description = "Delete a travel form submission")
    public ResponseEntity<ApiResponse<Void>> deleteTravelForm(@PathVariable Long id) {
        try {
            travelFormService.deleteTravelForm(id);
            return ResponseEntity.ok(ApiResponse.success("Travel form deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete travel form: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER') or hasRole('COORDINATOR')")
    @Operation(summary = "Get power portal dashboard stats", description = "Retrieve aggregated statistics for the power portal dashboard")
    public ResponseEntity<ApiResponse<PowerPortalDashboardResponse>> getDashboardStats() {
        PowerPortalDashboardResponse stats = travelFormService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", stats));
    }
}
