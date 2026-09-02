package com.powercity.power_city_platform.config;

import com.powercity.power_city_platform.entity.Role;
import com.powercity.power_city_platform.entity.RolePermission;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.repository.RolePermissionRepository;
import com.powercity.power_city_platform.repository.RoleRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import com.powercity.power_city_platform.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Map<String, String> DEFAULT_ROLES = Map.of(
        "SUPER_ADMIN", "Super Administrator with full system access",
        "ADMIN",       "Administrator with regional system access",
        "COORDINATOR", "Coordinator with limited administrative access",
        "NATIONAL_LEADER", "National Leader with regional access to forms, reports, and campus management",
        "USER",        "Standard user with basic access",
        "STUDENT",     "Student with access to courses and learning materials",
        "CUSTOMER",    "Customer with access to e-commerce features"
    );

    // Sidebar menu permissions. Seeded per role because Liquibase (which used to
    // populate role_permissions) is disabled — see application.yml. Without these,
    // the admin sidebar renders almost no menu items.
    private static final List<String> ALL_MENU_PERMISSIONS = List.of(
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

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
        "SUPER_ADMIN", ALL_MENU_PERMISSIONS,
        "ADMIN",       List.of("MENU_CAMPUSES", "MENU_SUBMISSIONS", "MENU_EVENTS"),
        "COORDINATOR", List.of("MENU_REPORTS"),
        "NATIONAL_LEADER", List.of("MENU_CAMPUSES", "MENU_SUBMISSIONS", "MENU_EVENTS", "MENU_REPORTS", "MENU_FORMS"),
        "USER",        List.of(),
        "STUDENT",     List.of(),
        "CUSTOMER",    List.of()
    );

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeRolePermissions();
        initializeSuperAdmin();
    }

    private void initializeRoles() {
        DEFAULT_ROLES.forEach((name, description) -> {
            if (!roleRepository.existsByName(name)) {
                roleRepository.save(new Role(name, description));
                System.out.println("Created role: " + name);
            }
        });
    }

    private void initializeRolePermissions() {
        ROLE_PERMISSIONS.forEach((roleName, permissionKeys) -> {
            for (String key : permissionKeys) {
                // Avoid unique-constraint errors on re-runs.
                boolean alreadyExists = rolePermissionRepository.findByRoleName(roleName)
                        .stream()
                        .anyMatch(rp -> rp.getPermissionKey().equals(key));
                if (!alreadyExists) {
                    rolePermissionRepository.save(new RolePermission(roleName, key));
                    System.out.println("Created permission: " + roleName + " -> " + key);
                }
            }
        });
    }

    private void initializeSuperAdmin() {
        // Multiple super-admin accounts are seeded so admins can log in across
        // environments regardless of which database (local vs deployed) is fresh.
        String[] superAdminEmails = {
                "it@drabeldamina.org",
                "admiglobalit@gmail.com"
        };

        for (String superAdminEmail : superAdminEmails) {
            if (userRepository.findByEmail(superAdminEmail).isEmpty()) {
                Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                        .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

                User superAdmin = new User();
                superAdmin.setEmail(superAdminEmail);
                superAdmin.setPassword(passwordEncoder.encode("Pciglobal@28$"));
                superAdmin.setFullName("Super Admin");
                superAdmin.setIsActive(true);
                superAdmin.setIsEmailVerified(true);

                User savedUser = userRepository.save(superAdmin);

                UserRole userRole = new UserRole();
                userRole.setUser(savedUser);
                userRole.setRole(superAdminRole);
                userRole.setIsActive(true);
                userRoleRepository.save(userRole);

                System.out.println("Created super admin user: " + superAdminEmail);
            }
        }
    }
}
