package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/matrix")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMatrix() {
        Map<String, Set<String>> matrix = permissionService.getPermissionMatrix();
        Map<String, Object> data = Map.of(
                "allPermissions", PermissionService.ALL_MENU_PERMISSIONS,
                "matrix", matrix
        );
        return ResponseEntity.ok(ApiResponse.success("Permission matrix loaded", data));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRolesWithPermissions() {
        List<Map<String, Object>> roles = permissionService.getRolesWithPermissions();
        return ResponseEntity.ok(ApiResponse.success("Roles loaded", roles));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRole(@RequestBody Map<String, Object> body) {
        String roleName = (String) body.get("roleName");
        String description = (String) body.getOrDefault("description", "");
        @SuppressWarnings("unchecked")
        Set<String> permissions = body.get("permissions") != null
                ? new HashSet<>((List<String>) body.get("permissions"))
                : Set.of();

        Map<String, Object> result = permissionService.createRole(roleName, description, permissions);
        return ResponseEntity.status(201).body(ApiResponse.success("Role created successfully", result));
    }

    @PutMapping("/roles/{roleName}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @PathVariable String roleName,
            @RequestBody Set<String> permissionKeys
    ) {
        permissionService.updateRolePermissions(roleName, permissionKeys);
        return ResponseEntity.ok(ApiResponse.success("Permissions updated for " + roleName, null));
    }

    @DeleteMapping("/roles/{roleName}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable String roleName) {
        permissionService.deleteRole(roleName);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
    }
}
