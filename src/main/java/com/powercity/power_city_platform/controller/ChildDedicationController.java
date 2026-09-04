package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.child.ChildDedicationCreateRequest;
import com.powercity.power_city_platform.dto.request.child.ChildDedicationEmailRequest;
import com.powercity.power_city_platform.dto.request.child.ChildDedicationStatusRequest;
import com.powercity.power_city_platform.dto.response.child.ChildDedicationResponse;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.service.ChildDedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/child-dedications")
@Tag(name = "Child Dedication Management", description = "APIs for managing child dedication certificates")
@RequiredArgsConstructor
public class ChildDedicationController {

    private final ChildDedicationService childDedicationService;

    @PostMapping
    @Operation(summary = "Submit a child dedication", description = "Create a new child dedication certificate")
    public ResponseEntity<ApiResponse<ChildDedicationResponse>> createDedication(
            @Valid @RequestBody ChildDedicationCreateRequest request) {
        try {
            ChildDedicationResponse response = childDedicationService.createDedication(request);
            return ResponseEntity.ok(ApiResponse.success("Child dedication certificate submitted successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to submit child dedication: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all child dedications", description = "Admins see all; national leaders/coordinators see their campus; regular users see only their own")
    public ResponseEntity<ApiResponse<List<ChildDedicationResponse>>> getAllDedications() {
        List<ChildDedicationResponse> dedications = childDedicationService.getAllDedications();
        return ResponseEntity.ok(ApiResponse.success("Child dedications retrieved successfully", dedications));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get child dedication by ID", description = "Retrieve details of a specific child dedication certificate")
    public ResponseEntity<ApiResponse<ChildDedicationResponse>> getDedicationById(@PathVariable Long id) {
        try {
            ChildDedicationResponse dedication = childDedicationService.getDedicationById(id);
            return ResponseEntity.ok(ApiResponse.success("Child dedication retrieved successfully", dedication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve child dedication: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/send-email")
    @Operation(summary = "Send child dedication certificate via email", description = "Email the child dedication certificate to the specified recipient")
    public ResponseEntity<ApiResponse<Void>> sendCertificateEmail(
            @PathVariable Long id,
            @Valid @RequestBody ChildDedicationEmailRequest request) {
        try {
            childDedicationService.sendCertificateEmail(id, request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Child dedication certificate sent via email successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update child dedication status", description = "Approve or reject a child dedication certificate (ADMIN only)")
    public ResponseEntity<ApiResponse<ChildDedicationResponse>> updateDedicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChildDedicationStatusRequest request) {
        try {
            ChildDedicationResponse dedication = childDedicationService.updateStatus(id, request.getStatus(), request.getRejectionReason());
            return ResponseEntity.ok(ApiResponse.success("Child dedication " + request.getStatus().toLowerCase() + " successfully", dedication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER') or hasRole('COORDINATOR')")
    @Operation(summary = "Delete child dedication", description = "Delete a child dedication certificate")
    public ResponseEntity<ApiResponse<Void>> deleteDedication(@PathVariable Long id) {
        try {
            childDedicationService.deleteDedication(id);
            return ResponseEntity.ok(ApiResponse.success("Child dedication deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete child dedication: " + e.getMessage()));
        }
    }
}
