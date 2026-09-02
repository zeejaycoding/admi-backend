package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.user.*;
import com.powercity.power_city_platform.dto.response.user.*;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User profile and management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Retrieve the profile of the authenticated user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile by ID", description = "Retrieve profile of a specific user (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update the authenticated user's profile information")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserProfileResponse profile = userService.updateUserProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change the authenticated user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }


    @GetMapping("/dashboard")
    @Operation(summary = "Get user dashboard", description = "Retrieve dashboard data for the authenticated user")
    public ResponseEntity<ApiResponse<UserDashboardResponse>> getUserDashboard() {
        UserDashboardResponse dashboard = userService.getUserDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully", dashboard));
    }


    @GetMapping("/all")
    @Operation(summary = "Get all users", description = "Get all users with pagination (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    public ResponseEntity<ApiResponse<Page<UserListResponse>>> getAllUsers(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDirection) {
        UserSearchRequest searchRequest = new UserSearchRequest(
                null, null, null, null,
                null, null, null, page, size, sortBy, sortDirection);

        Page<UserListResponse> users = userService.searchUsers(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("All users retrieved successfully", users));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search and filter users (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserListResponse>>> searchUsers(
            @Parameter(description = "Search term") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "First name filter") @RequestParam(required = false) String firstName,
            @Parameter(description = "Last name filter") @RequestParam(required = false) String lastName,
            @Parameter(description = "Email filter") @RequestParam(required = false) String email,
            @Parameter(description = "Enabled status filter") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Email verified filter") @RequestParam(required = false) Boolean emailVerified,
            @Parameter(description = "Account locked filter") @RequestParam(required = false) Boolean accountLocked,
            @Parameter(description = "Role filter") @RequestParam(required = false) String role,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDirection) {
        String fullName = (firstName != null || lastName != null)
                ? ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim()
                : null;

        UserSearchRequest searchRequest = new UserSearchRequest(
                searchTerm, fullName, email, enabled,
                emailVerified, accountLocked, role, page, size, sortBy, sortDirection);

        Page<UserListResponse> users = userService.searchUsers(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }


    @PostMapping("/roles/assign")
    @Operation(summary = "Assign role to user", description = "Assign a role to a user (Admin only)")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @Valid @RequestBody AssignRoleRequest request) {
        userService.assignRole(request);
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully"));
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @Operation(summary = "Remove role from user", description = "Remove a role from a user (Admin only)")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Role name", required = true) @PathVariable String roleName) {
        userService.removeRole(userId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully"));
    }

    @GetMapping("/{userId}/roles")
    @Operation(summary = "Get user roles", description = "Get all roles assigned to a user (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleAssignmentResponse>>> getUserRoles(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        List<RoleAssignmentResponse> roles = userService.getUserRoles(userId);
        return ResponseEntity.ok(ApiResponse.success("User roles retrieved successfully", roles));
    }


    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user account", description = "Activate a user account (Admin only)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User account activated successfully"));
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate user account", description = "Deactivate a user account (Admin only)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User account deactivated successfully"));
    }


    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Get platform user statistics (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserPlatformStatsResponse>> getUserStats() {
        UserPlatformStatsResponse stats = userService.getPlatformUserStats();
        return ResponseEntity.ok(ApiResponse.success("User statistics retrieved successfully", stats));
    }

    @GetMapping("/activity")
    @Operation(summary = "Get user activity", description = "Get detailed user activity for the authenticated user")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RecentActivityResponse>>> getUserActivity(
            @Parameter(description = "Number of activities to retrieve") @RequestParam(defaultValue = "10") int limit) {
        List<RecentActivityResponse> activities = userService.getUserActivity(limit);
        return ResponseEntity.ok(ApiResponse.success("User activity retrieved successfully", activities));
    }
}
