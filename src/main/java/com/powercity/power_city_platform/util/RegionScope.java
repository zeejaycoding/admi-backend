package com.powercity.power_city_platform.util;

import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility to determine the data-region scope for the current authenticated user.
 *
 * SUPER_ADMIN / ADMIN  → returns null  (no region restriction; sees everything).
 * NATIONAL_LEADER      → returns the user's {@code region} value (e.g. "Nigeria").
 * Everyone else        → returns null  (not restricted at this layer; other
 *                         per-service logic handles their narrower scope).
 */
public final class RegionScope {

    private RegionScope() {}

    /**
     * Returns the region the current user is restricted to, or {@code null}
     * if the user has unrestricted (global) access.
     */
    public static String resolve(UserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;

        Optional<User> userOpt = userRepository.findByEmail(auth.getName());
        if (userOpt.isEmpty()) return null;

        User user = userOpt.get();
        boolean isNationalLeader = user.getUserRoles() != null
                && user.getUserRoles().stream()
                    .anyMatch(ur -> ur.getRole() != null
                            && "NATIONAL_LEADER".equals(ur.getRole().getName()));

        if (!isNationalLeader) return null;

        return user.getRegion();
    }

    /**
     * Convenience check: is the current user a NATIONAL_LEADER?
     */
    public static boolean isNationalLeader(UserRepository userRepository) {
        return resolve(userRepository) != null;
    }
}
