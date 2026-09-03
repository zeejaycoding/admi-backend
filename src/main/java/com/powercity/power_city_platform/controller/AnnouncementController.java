package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.announcement.CreateAnnouncementRequest;
import com.powercity.power_city_platform.dto.response.announcement.AnnouncementResponse;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.AnnouncementService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/announcements")
@Tag(name = "Announcements", description = "APIs for regional announcements and communications")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    @Operation(summary = "Create announcement", description = "Send an announcement to campus coordinators and leaders in a region")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request) {
        User currentUser = userService.getCurrentUser();
        AnnouncementResponse created = announcementService.createAnnouncement(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement sent successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER') or hasRole('COORDINATOR')")
    @Operation(summary = "Get announcements", description = "Get all announcements scoped to the current user's region")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getAllAnnouncements() {
        User currentUser = userService.getCurrentUser();
        List<AnnouncementResponse> announcements = announcementService.getAllAnnouncements(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved successfully", announcements));
    }

    @GetMapping("/recipient")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER') or hasRole('COORDINATOR')")
    @Operation(summary = "Get announcements for recipient", description = "Get announcements relevant to the current user's region")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getRecipientAnnouncements() {
        User currentUser = userService.getCurrentUser();
        List<AnnouncementResponse> announcements = announcementService.getAnnouncementsForRecipient(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved successfully", announcements));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncementById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        AnnouncementResponse announcement = announcementService.getAnnouncementById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement retrieved successfully", announcement));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    @Operation(summary = "Delete announcement", description = "Delete an announcement (Admin or National Leader only)")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        announcementService.deleteAnnouncement(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted successfully", null));
    }
}
