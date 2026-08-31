package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.entity.Role;
import com.powercity.power_city_platform.entity.RolePermission;
import com.powercity.power_city_platform.repository.RolePermissionRepository;
import com.powercity.power_city_platform.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    public static final List<String> ALL_MENU_PERMISSIONS = List.of(
            "MENU_OVERVIEW",
            "MENU_USERS",
            "MENU_CAMPUSES",
            "MENU_BOOKS",
            "MENU_COURSES",
            "MENU_ORDERS",
            "MENU_PAYMENTS",
            "MENU_FORMS",
            "MENU_SUBMISSIONS",
            "MENU_REPORTS",
            "MENU_EVENTS",
            "MENU_MANAGEMENT"
    );

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;

    public PermissionService(RolePermissionRepository rolePermissionRepository, RoleRepository roleRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
    }

    public Set<String> getPermissionsForRole(String roleName) {
        return rolePermissionRepository.findByRoleName(roleName)
                .stream()
                .map(RolePermission::getPermissionKey)
                .collect(Collectors.toSet());
    }

    public Set<String> getEffectivePermissions(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) return Set.of();
        return rolePermissionRepository.findPermissionKeysByRoleNames(roleNames);
    }

    public Map<String, Set<String>> getPermissionMatrix() {
        Map<String, Set<String>> matrix = new LinkedHashMap<>();
        for (Role role : roleRepository.findAll()) {
            matrix.put(role.getName(), getPermissionsForRole(role.getName()));
        }
        return matrix;
    }

    public List<Map<String, Object>> getRolesWithPermissions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role role : roleRepository.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", role.getName());
            row.put("label", toLabel(role.getName()));
            row.put("description", role.getDescription() != null ? role.getDescription() : "");
            row.put("permissions", getPermissionsForRole(role.getName()));
            result.add(row);
        }
        return result;
    }

    public List<String> getAvailableRoleNames() {
        return roleRepository.findAll()
                .stream()
                .map(Role::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createRole(String roleName, String description, Set<String> permissionKeys) {
        String normalizedName = roleName.toUpperCase().replaceAll("\\s+", "_");

        if (roleRepository.existsByName(normalizedName)) {
            throw new IllegalStateException("Role already exists: " + normalizedName);
        }

        Role role = new Role(normalizedName, description);
        roleRepository.save(role);

        if (permissionKeys != null && !permissionKeys.isEmpty()) {
            Set<String> validKeys = new HashSet<>(ALL_MENU_PERMISSIONS);
            List<RolePermission> perms = permissionKeys.stream()
                    .filter(validKeys::contains)
                    .map(key -> new RolePermission(normalizedName, key))
                    .collect(Collectors.toList());
            rolePermissionRepository.saveAll(perms);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", normalizedName);
        result.put("label", toLabel(normalizedName));
        result.put("description", description != null ? description : "");
        result.put("permissions", permissionKeys != null ? permissionKeys : Set.of());
        return result;
    }

    @Transactional
    public void deleteRole(String roleName) {
        rolePermissionRepository.deleteByRoleName(roleName);
        roleRepository.findByName(roleName).ifPresent(roleRepository::delete);
    }

    @Transactional
    public void updateRolePermissions(String roleName, Set<String> permissionKeys) {
        Set<String> validKeys = new HashSet<>(ALL_MENU_PERMISSIONS);
        Set<String> filtered = permissionKeys.stream()
                .filter(validKeys::contains)
                .collect(Collectors.toSet());

        rolePermissionRepository.deleteByRoleName(roleName);

        List<RolePermission> newPermissions = filtered.stream()
                .map(key -> new RolePermission(roleName, key))
                .collect(Collectors.toList());

        rolePermissionRepository.saveAll(newPermissions);
    }

    private String toLabel(String roleName) {
        return Arrays.stream(roleName.split("_"))
                .map(w -> w.charAt(0) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
