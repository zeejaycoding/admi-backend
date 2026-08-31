package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.RoleService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Role Management", description = "APIs for managing user roles (Admin only)")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all roles", description = "Get all available roles")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved", roleService.getAllRoles()));
    }

    @GetMapping("/users/{roleName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get users by role", description = "Get all users with a specific role")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsersByRole(
            @PathVariable String roleName) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", roleService.getUsersByRole(roleName)));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign role", description = "Assign a role to a user")
    public ResponseEntity<ApiResponse<Void>> assignRole(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String roleName = request.get("roleName").toString();
        User currentUser = userService.getCurrentUser();
        roleService.assignRole(userId, roleName, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully", null));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Remove role", description = "Remove a role from a user")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        roleService.removeRole(userId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", null));
    }
}
