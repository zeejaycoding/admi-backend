package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.Role;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.RoleRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import com.powercity.power_city_platform.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public List<Map<String, Object>> getAllRoles() {
        return roleRepository.findAll().stream().map(role -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", role.getName());
            map.put("description", getDescription(role.getName()));
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getUsersByRole(String roleName) {
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        return userRoleRepository.findByRoleAndIsActiveTrue(role).stream()
                .map(ur -> {
                    User u = ur.getUser();
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("email", u.getEmail());
                    map.put("firstName", u.getFirstName());
                    map.put("lastName", u.getLastName());
                    map.put("region", u.getRegion());
                    map.put("isActive", u.getIsActive());
                    return map;
                }).collect(Collectors.toList());
    }

    public void assignRole(Long userId, String roleName, User currentUser) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (userRoleRepository.existsByUserAndRole(targetUser, role)) {
            throw new RuntimeException("User already has this role");
        }

        UserRole userRole = new UserRole(targetUser, role);
        userRole.setCreatedBy(currentUser.getId());
        userRoleRepository.save(userRole);
    }

    public void removeRole(Long userId, String roleName) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        UserRole userRole = userRoleRepository.findByUserAndRole(targetUser, role)
                .orElseThrow(() -> new ResourceNotFoundException("User does not have this role"));

        userRole.setIsActive(false);
        userRoleRepository.save(userRole);
    }

    private String getDescription(String roleName) {
        return switch (roleName.toUpperCase()) {
            case "SUPER_ADMIN" -> "Full system access";
            case "ADMIN" -> "Regional system access";
            case "COORDINATOR" -> "Limited administrative access";
            case "USER" -> "Basic access";
            case "STUDENT" -> "Course and learning access";
            case "CUSTOMER" -> "E-commerce access";
            case "NATIONAL_LEADER" -> "Event management privileges";
            default -> "";
        };
    }
}
