package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.marriage.MarriageCertificateCreateRequest;
import com.powercity.power_city_platform.dto.request.marriage.MarriageCertificateStatusRequest;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.dto.response.marriage.MarriageCertificateResponse;
import com.powercity.power_city_platform.service.MarriageCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marriage-certificates")
@Tag(name = "Marriage Certificate Management", description = "APIs for managing marriage certificates")
@RequiredArgsConstructor
public class MarriageCertificateController {

    private final MarriageCertificateService marriageCertificateService;

    @PostMapping
    @Operation(summary = "Create a marriage certificate", description = "Save the marriage certificate and email it to both partners")
    public ResponseEntity<ApiResponse<MarriageCertificateResponse>> createCertificate(
            @Valid @RequestBody MarriageCertificateCreateRequest request) {
        try {
            MarriageCertificateResponse response = marriageCertificateService.createCertificate(request);
            return ResponseEntity.ok(ApiResponse.success("Marriage certificate saved and sent to both partners successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create marriage certificate: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all marriage certificates", description = "Admins see all; regular users see only their own")
    public ResponseEntity<ApiResponse<List<MarriageCertificateResponse>>> getAllCertificates() {
        try {
            List<MarriageCertificateResponse> certificates = marriageCertificateService.getAllCertificates();
            return ResponseEntity.ok(ApiResponse.success("Marriage certificates retrieved successfully", certificates));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve marriage certificates: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get marriage certificate by ID", description = "Retrieve details of a specific marriage certificate")
    public ResponseEntity<ApiResponse<MarriageCertificateResponse>> getCertificateById(@PathVariable Long id) {
        try {
            MarriageCertificateResponse certificate = marriageCertificateService.getCertificateById(id);
            return ResponseEntity.ok(ApiResponse.success("Marriage certificate retrieved successfully", certificate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve marriage certificate: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update marriage certificate status", description = "Approve or reject a marriage certificate (ADMIN only)")
    public ResponseEntity<ApiResponse<MarriageCertificateResponse>> updateCertificateStatus(
            @PathVariable Long id,
            @Valid @RequestBody MarriageCertificateStatusRequest request) {
        try {
            MarriageCertificateResponse certificate = marriageCertificateService.updateStatus(
                    id, request.getStatus(), request.getRejectionReason());
            return ResponseEntity.ok(ApiResponse.success("Marriage certificate " + request.getStatus().toLowerCase() + " successfully", certificate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER') or hasRole('COORDINATOR')")
    @Operation(summary = "Delete marriage certificate", description = "Delete a marriage certificate")
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(@PathVariable Long id) {
        try {
            marriageCertificateService.deleteCertificate(id);
            return ResponseEntity.ok(ApiResponse.success("Marriage certificate deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete marriage certificate: " + e.getMessage()));
        }
    }
}
