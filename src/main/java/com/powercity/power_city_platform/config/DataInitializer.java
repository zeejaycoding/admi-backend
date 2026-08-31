package com.powercity.power_city_platform.config;

import com.powercity.power_city_platform.entity.Role;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.repository.RoleRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import com.powercity.power_city_platform.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    private PasswordEncoder passwordEncoder;

    private static final Map<String, String> DEFAULT_ROLES = Map.of(
        "SUPER_ADMIN", "Super Administrator with full system access",
        "ADMIN",       "Administrator with regional system access",
        "COORDINATOR", "Coordinator with limited administrative access",
        "USER",        "Standard user with basic access",
        "STUDENT",     "Student with access to courses and learning materials",
        "CUSTOMER",    "Customer with access to e-commerce features"
    );

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
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

    private void initializeSuperAdmin() {
        String superAdminEmail = "it@drabeldamina.org";

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
