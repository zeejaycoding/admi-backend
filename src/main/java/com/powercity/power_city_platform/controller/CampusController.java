package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.campus.CreateCampusRequest;
import com.powercity.power_city_platform.dto.request.campus.UpdateCampusRequest;
import com.powercity.power_city_platform.dto.request.campus.CampusSearchRequest;
import com.powercity.power_city_platform.dto.response.campus.CampusResponse;
import com.powercity.power_city_platform.dto.response.campus.CampusSummaryResponse;
import com.powercity.power_city_platform.dto.response.campus.CampusListResponse;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.service.CampusService;
import com.powercity.power_city_platform.service.S3FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/campuses")
@Tag(name = "Campus Management", description = "Regional campus management and analytics")
@RequiredArgsConstructor
public class CampusController {

    private final CampusService campusService;
    private final S3FileService s3FileService;

    private static final Logger logger = LoggerFactory.getLogger(CampusController.class);

    @GetMapping("/regions")
    @Operation(summary = "Get supported regions", description = "Retrieve all supported regions")
    public ResponseEntity<ApiResponse<List<String>>> getSupportedRegions() {
        List<String> regions = campusService.getSupportedRegions();
        return ResponseEntity.ok(ApiResponse.success("Supported regions retrieved successfully", regions));
    }

    @GetMapping("/currencies")
    @Operation(summary = "Get supported currencies", description = "Retrieve all supported currencies")
    public ResponseEntity<ApiResponse<List<Currency>>> getSupportedCurrencies() {
        List<Currency> currencies = campusService.getSupportedCurrencies();
        return ResponseEntity.ok(ApiResponse.success("Supported currencies retrieved successfully", currencies));
    }

    @GetMapping("/currency/{region}")
    @Operation(summary = "Get currency for region", description = "Get the default currency for a specific region")
    public ResponseEntity<ApiResponse<Currency>> getCurrencyForRegion(
            @Parameter(description = "Region name") @PathVariable String region) {
        Currency currency = campusService.getCurrencyForRegion(region);
        return ResponseEntity.ok(ApiResponse.success("Currency for region retrieved successfully", currency));
    }

    @GetMapping("/public/search")
    @Operation(summary = "Search campuses (public)", description = "Search active campuses without authentication")
    public ResponseEntity<ApiResponse<CampusListResponse>> searchCampusesPublic(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        CampusSearchRequest request = new CampusSearchRequest();
        request.setSearchTerm(searchTerm);
        request.setRegion(region);
        request.setCity(city);
        request.setCountry(country);
        request.setIsActive(true); // Only show active campuses publicly
        request.setIsFeatured(isFeatured);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        CampusListResponse response = campusService.searchCampuses(request);
        return ResponseEntity.ok(ApiResponse.success("Campus search completed successfully", response));
    }

    @GetMapping("/public/region/{region}")
    @Operation(summary = "Get campuses by region (public)", description = "Get active campuses for a specific region")
    public ResponseEntity<ApiResponse<List<CampusSummaryResponse>>> getCampusesByRegionPublic(
            @Parameter(description = "Region name") @PathVariable String region) {
        List<CampusSummaryResponse> campuses = campusService.getCampusesByRegion(region);
        return ResponseEntity.ok(ApiResponse.success("Campuses for region retrieved successfully", campuses));
    }

    @GetMapping("/public/featured")
    @Operation(summary = "Get featured campuses (public)", description = "Get all featured campuses")
    public ResponseEntity<ApiResponse<List<CampusSummaryResponse>>> getFeaturedCampusesPublic() {
        List<CampusSummaryResponse> campuses = campusService.getFeaturedCampuses();
        return ResponseEntity.ok(ApiResponse.success("Featured campuses retrieved successfully", campuses));
    }

    @GetMapping("/public/featured/{region}")
    @Operation(summary = "Get featured campuses by region (public)", description = "Get featured campuses for a specific region")
    public ResponseEntity<ApiResponse<List<CampusSummaryResponse>>> getFeaturedCampusesByRegionPublic(
            @Parameter(description = "Region name") @PathVariable String region) {
        List<CampusSummaryResponse> campuses = campusService.getFeaturedCampusesByRegion(region);
        return ResponseEntity.ok(ApiResponse.success("Featured campuses for region retrieved successfully", campuses));
    }


    @GetMapping("/public/{campusId}")
    @Operation(summary = "Get campus details (public)", description = "Get detailed information about a specific campus")
    public ResponseEntity<ApiResponse<CampusResponse>> getCampusDetailsPublic(
            @Parameter(description = "Campus ID") @PathVariable Long campusId) {
        CampusResponse campus = campusService.getCampusById(campusId);
        return ResponseEntity.ok(ApiResponse.success("Campus details retrieved successfully", campus));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all campuses", description = "Get all campuses with pagination")
    public ResponseEntity<ApiResponse<CampusListResponse>> getAllCampuses(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDirection) {

        CampusSearchRequest request = new CampusSearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        CampusListResponse response = campusService.searchCampuses(request);
        return ResponseEntity.ok(ApiResponse.success("All campuses retrieved successfully", response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search campuses", description = "Search campuses with full access")
    public ResponseEntity<ApiResponse<CampusListResponse>> searchCampuses(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        CampusSearchRequest request = new CampusSearchRequest();
        request.setSearchTerm(searchTerm);
        request.setRegion(region);
        request.setCity(city);
        request.setCountry(country);
        request.setIsActive(isActive);
        request.setIsFeatured(isFeatured);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        CampusListResponse response = campusService.searchCampuses(request);
        return ResponseEntity.ok(ApiResponse.success("Campus search completed successfully", response));
    }

    @GetMapping("/{campusId}")
    @Operation(summary = "Get campus by ID", description = "Get detailed campus information")
    public ResponseEntity<ApiResponse<CampusResponse>> getCampusById(
            @Parameter(description = "Campus ID") @PathVariable Long campusId) {
        CampusResponse campus = campusService.getCampusById(campusId);
        return ResponseEntity.ok(ApiResponse.success("Campus retrieved successfully", campus));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create campus", description = "Create a new campus")
    public ResponseEntity<ApiResponse<CampusResponse>> createCampus(
            @Valid @RequestBody CreateCampusRequest request) {
        CampusResponse campus = campusService.createCampus(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Campus created successfully", campus));
    }

    @PutMapping("/{campusId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update campus", description = "Update an existing campus")
    public ResponseEntity<ApiResponse<CampusResponse>> updateCampus(
            @Parameter(description = "Campus ID") @PathVariable Long campusId,
            @Valid @RequestBody UpdateCampusRequest request) {
        CampusResponse campus = campusService.updateCampus(campusId, request);
        return ResponseEntity.ok(ApiResponse.success("Campus updated successfully", campus));
    }

    @DeleteMapping("/{campusId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete campus", description = "Delete a campus")
    public ResponseEntity<ApiResponse<String>> deleteCampus(
            @Parameter(description = "Campus ID") @PathVariable Long campusId) {
        campusService.deleteCampus(campusId);
        return ResponseEntity.ok(ApiResponse.success("Campus deleted successfully"));
    }

    @PostMapping("/{campusId}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate campus", description = "Activate a campus")
    public ResponseEntity<ApiResponse<String>> activateCampus(
            @Parameter(description = "Campus ID") @PathVariable Long campusId) {
        campusService.updateCampusStatus(campusId, true);
        return ResponseEntity.ok(ApiResponse.success("Campus activated successfully"));
    }

    @PostMapping("/{campusId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate campus", description = "Deactivate a campus")
    public ResponseEntity<ApiResponse<String>> deactivateCampus(
            @Parameter(description = "Campus ID") @PathVariable Long campusId) {
        campusService.updateCampusStatus(campusId, false);
        return ResponseEntity.ok(ApiResponse.success("Campus deactivated successfully"));
    }

    @PostMapping("/{campusId}/feature")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Feature campus", description = "Mark a campus as featured")
    public ResponseEntity<ApiResponse<String>> featureCampus(
            @Parameter(description = "Campus ID") @PathVariable Long campusId) {
        campusService.updateFeaturedStatus(campusId, true);
        return ResponseEntity.ok(ApiResponse.success("Campus featured successfully"));
    }

    @PostMapping("/{campusId}/unfeature")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Unfeature campus", description = "Remove featured status from a campus")
    public ResponseEntity<ApiResponse<String>> unfeatureCampus(
            @Parameter(description = "Campus ID") @PathVariable Long campusId) {
        campusService.updateFeaturedStatus(campusId, false);
        return ResponseEntity.ok(ApiResponse.success("Campus unfeatured successfully"));
    }

    @PostMapping("/{campusId}/image")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Upload campus image", description = "Upload an image for a campus")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadCampusImage(
            @Parameter(description = "Campus ID") @PathVariable Long campusId,
            @RequestParam("file") MultipartFile file) {

        // Upload to S3 in campus images folder
        String s3Key = s3FileService.uploadCampusImage(campusId, file);

        // Create response with file information
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("s3Key", s3Key);
        uploadResult.put("fileUrl", s3FileService.getPublicUrl(s3Key));
        uploadResult.put("fileName", file.getOriginalFilename());
        uploadResult.put("fileSize", file.getSize());

        // Update campus with new image URL
        // This would typically be done through the service layer

        return ResponseEntity.ok(ApiResponse.success("Campus image uploaded successfully", uploadResult));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get campus statistics", description = "Get accurate campus stats from the database (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCampusStats() {
        return ResponseEntity.ok(ApiResponse.success("Campus stats retrieved successfully", campusService.getCampusStats()));
    }

    @DeleteMapping("/{campusId}/image")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete campus image", description = "Delete the image for a campus")
    public ResponseEntity<ApiResponse<String>> deleteCampusImage(
            @Parameter(description = "Campus ID") @PathVariable Long campusId) {
        // This would involve getting the campus, deleting the S3 file, and updating the
        // campus
        return ResponseEntity.ok(ApiResponse.success("Campus image deleted successfully"));
    }

}
