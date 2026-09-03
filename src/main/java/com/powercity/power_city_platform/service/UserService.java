package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.user.*;
import com.powercity.power_city_platform.dto.response.user.*;
import com.powercity.power_city_platform.entity.Role;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.RoleRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import com.powercity.power_city_platform.repository.UserRoleRepository;
import com.powercity.power_city_platform.repository.CampusRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CampusRepository campusRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3FileService s3FileService;
    private final PermissionService permissionService;

    // Get system user for automated operations
    public User getSystemUser() {
        // Try to find existing system user
        Optional<User> systemUser = userRepository.findByEmail("system@powercity.com");

        if (systemUser.isPresent()) {
            return systemUser.get();
        }

        // Create system user if it doesn't exist
        User newSystemUser = new User();
        newSystemUser.setEmail("system@powercity.com");
        newSystemUser.setFullName("System User");
        newSystemUser.setPassword(passwordEncoder.encode("system-password-" + System.currentTimeMillis()));
        newSystemUser.setIsActive(true);
        newSystemUser.setRegion("GLOBAL");

        User savedSystemUser = userRepository.save(newSystemUser);

        // Assign SUPER_ADMIN role to system user
        Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("SUPER_ADMIN role not found"));

        UserRole userRole = new UserRole(savedSystemUser, superAdminRole);
        userRoleRepository.save(userRole);

        return savedSystemUser;
    }

    // Get a user by email for websocket handshake (returns null if not found)
    @Transactional(readOnly = true)
    public User getUserByEmailForWs(String email) {
        return userRepository.findActiveUserByEmail(email).orElse(null);
    }

    // Get current authenticated user
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    // Get user profile
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return createUserProfileResponse(user);
    }

    // Get current user profile
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        return createUserProfileResponse(getCurrentUser());
    }

    // Update user profile
    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest request) {
        User currentUser = getCurrentUser();

        // Update fields if provided
        if (request.fullName() != null) {
            currentUser.setFullName(request.fullName());
        }
        if (request.phoneNumber() != null) {
            currentUser.setPhoneNumber(request.phoneNumber());
        }
        if (request.region() != null) {
            currentUser.setRegion(request.region());
            currentUser.setCurrency(getCurrencyByRegion(request.region()));
        }
        if (request.bio() != null) {
            currentUser.setBio(request.bio());
        }
        if (request.profileImageUrl() != null) {
            currentUser.setProfileImageUrl(request.profileImageUrl());
        }

        User savedUser = userRepository.save(currentUser);
        return createUserProfileResponse(savedUser);
    }

    // Change password
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = getCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(currentUser);
    }


    /**
     * Update user profile image
     */
    public void updateProfileImage(Long userId, String imageUrl, String s3Key) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Delete old profile image if exists
        if (user.getProfileImageKey() != null && !user.getProfileImageKey().isEmpty()) {
            try {
                s3FileService.deleteFile(user.getProfileImageKey());
            } catch (Exception e) {
                // Log warning but don't fail the update
                logger.error("Warning: Failed to delete old profile image: {}", e.getMessage(), e);
            }
        }

        user.setProfileImageUrl(imageUrl);
        user.setProfileImageKey(s3Key);
        userRepository.save(user);
    }

    /**
     * Delete user profile image
     */
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProfileImageKey() != null && !user.getProfileImageKey().isEmpty()) {
            try {
                s3FileService.deleteFile(user.getProfileImageKey());
            } catch (Exception e) {
                // Log warning but don't fail the delete
                logger.error("Warning: Failed to delete profile image from S3: {}", e.getMessage(), e);
            }
        }

        user.setProfileImageUrl(null);
        user.setProfileImageKey(null);
        userRepository.save(user);
    }

    /**
     * Get presigned URL for user's profile image
     */
    @Transactional(readOnly = true)
    public String getProfileImagePresignedUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProfileImageKey() == null || user.getProfileImageKey().isEmpty()) {
            return null;
        }

        try {
            return s3FileService.generatePresignedUrl(user.getProfileImageKey(), java.time.Duration.ofHours(1));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    /**
     * Get user's profile image thumbnail URL
     */
    @Transactional(readOnly = true)
    public String getProfileImageThumbnailUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProfileImageKey() == null || user.getProfileImageKey().isEmpty()) {
            return null;
        }

        try {
            // Generate thumbnail key from original key
            String thumbnailKey = user.getProfileImageKey().replace("profile-images/", "profile-images/thumbnails/");
            return s3FileService.generatePresignedUrl(thumbnailKey, java.time.Duration.ofHours(1));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate thumbnail URL: " + e.getMessage());
        }
    }

    // Get user dashboard
    @Transactional(readOnly = true)
    public UserDashboardResponse getUserDashboard() {
        User currentUser = getCurrentUser();
        UserProfileResponse profile = createUserProfileResponse(currentUser);
        UserStatsResponse stats = getUserStats(currentUser.getId());
        List<RecentActivityResponse> recentActivities = getRecentActivities(currentUser.getId());
        List<String> recommendations = getRecommendations(currentUser);
        return new UserDashboardResponse(profile, recentActivities, stats, recommendations);
    }

    // Search users (Admin function)
    @Transactional(readOnly = true)
    public Page<UserListResponse> searchUsers(UserSearchRequest request) {
        // TODO: Implement custom repository method for complex search
        Pageable pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.Direction.fromString(request.sortDirection()),
                request.sortBy());

        User currentUser = getCurrentUser();
        if (isNationalLeader(currentUser)) {
            // NL sees only coordinators within their own region
            List<User> coordinators = userRepository.findCoordinators(
                    currentUser.getRegion(), request.searchTerm());
            int total = coordinators.size();
            int from = (int) Math.min(pageable.getOffset(), total);
            int to = (int) Math.min(from + pageable.getPageSize(), total);
            List<UserListResponse> content = coordinators.subList(from, to).stream()
                    .map(this::createUserListResponse)
                    .collect(Collectors.toList());
            return new PageImpl<>(content, pageable, total);
        }

        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::createUserListResponse);
    }

    private boolean isNationalLeader(User user) {
        if (user == null || user.getUserRoles() == null) return false;
        return user.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .map(ur -> ur.getRole().getName())
                .anyMatch(name -> "NATIONAL_LEADER".equalsIgnoreCase(name));
    }

    // Create user directly (Super Admin function)
    public UserProfileResponse createUser(CreateUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("A user with this email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber());
        user.setIsEmailVerified(request.emailVerified() != null ? request.emailVerified() : true);
        user.setIsActive(request.enabled() != null ? request.enabled() : true);
        user.setCreatedByAdmin(true);

        User savedUser = userRepository.save(user);

        for (String roleName : request.roles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            UserRole userRole = new UserRole(savedUser, role);
            userRoleRepository.save(userRole);
        }

        // Reload user to include roles
        User userWithRoles = userRepository.findById(savedUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after creation"));

        return createUserProfileResponse(userWithRoles);
    }

    // Get all available roles (Admin function)
    @Transactional(readOnly = true)
    public List<Map<String, String>> getAvailableRoles() {
        return roleRepository.findAll().stream()
                .map(role -> Map.of(
                        "roleName", role.getName(),
                        "label", formatRoleLabel(role.getName())
                ))
                .collect(Collectors.toList());
    }

    private String formatRoleLabel(String roleName) {
        return Arrays.stream(roleName.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    // Assign role to user (Admin function)
    public void assignRole(AssignRoleRequest request) {
        User currentUser = getCurrentUser();

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(request.roleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Check if user already has this role
        if (userRoleRepository.existsByUserAndRole(targetUser, role)) {
            throw new RuntimeException("User already has this role");
        }

        // Create role assignment
        UserRole userRole = new UserRole(targetUser, role);
        userRole.setCreatedBy(currentUser.getId());
        userRoleRepository.save(userRole);
    }

    // Remove role from user (Admin function)
    public void removeRole(Long userId, String roleName) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        UserRole userRole = userRoleRepository.findByUserAndRole(targetUser, role)
                .orElseThrow(() -> new ResourceNotFoundException("User does not have this role"));

        userRole.setIsActive(false);
        userRoleRepository.save(userRole);
    }

    // Get user roles
    @Transactional(readOnly = true)
    public List<RoleAssignmentResponse> getUserRoles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return user.getUserRoles().stream()
                .map(this::createRoleAssignmentResponse)
                .collect(Collectors.toList());
    }

    // Deactivate user account
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);
    }

    // Tag a user record for admin review
    public void tagForReview(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFlaggedForReview(true);
        user.setReviewTagReason(reason);
        user.setReviewTaggedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Clear the review tag from a user record
    public void clearReviewTag(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFlaggedForReview(false);
        user.setReviewTagReason(null);
        user.setReviewTaggedAt(null);
        userRepository.save(user);
    }

    // Reset user password (Admin function)
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.resetFailedLoginAttempts();
        userRepository.save(user);
    }

    // Activate user account
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(true);
        userRepository.save(user);
    }

    private UserProfileResponse createUserProfileResponse(User user) {
        Set<String> roles = new HashSet<>();
        if (user.getUserRoles() != null) {
            roles = user.getUserRoles().stream()
                    .filter(UserRole::getIsActive)
                    .map(userRole -> userRole.getRole().getName())
                    .collect(Collectors.toSet());
        }

        Set<String> permissions = permissionService.getEffectivePermissions(roles);
        UserStatsResponse stats = getUserStats(user.getId());

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRegion(),
                user.getCurrency(),
                user.getBio(),
                user.getProfileImageUrl(),
                user.getIsEmailVerified(),
                user.getIsActive(),
                roles,
                permissions,
                user.getLastLogin(),
                user.getCreatedAt(),
                stats,
                resolveCampusName(user.getEmail()));
    }

    public String resolveCampusName(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try {
            return campusRepository.findFirstByCoordinatorEmailIgnoreCaseAndIsActiveTrue(email)
                    .map(campus -> campus.getName())
                    .orElse(null);
        } catch (Exception e) {
            logger.warn("Failed to resolve campus for email {}: {}", email, e.getMessage());
            return null;
        }
    }

    private UserListResponse createUserListResponse(User user) {
        Set<String> roles = new HashSet<>();
        if (user.getUserRoles() != null) {
            roles = user.getUserRoles().stream()
                    .filter(UserRole::getIsActive)
                    .map(userRole -> userRole.getRole().getName())
                    .collect(Collectors.toSet());
        }

        return new UserListResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRegion(),
                roles,
                user.getIsActive(),
                user.getIsEmailVerified(),
                user.getCreatedByAdmin() != null && user.getCreatedByAdmin(),
                user.getLastLogin(),
                user.getCreatedAt(),
                user.getFlaggedForReview() != null && user.getFlaggedForReview(),
                user.getReviewTagReason(),
                user.getReviewTaggedAt(),
                resolveCampusName(user.getEmail()));
    }

    private RoleAssignmentResponse createRoleAssignmentResponse(UserRole userRole) {
        return new RoleAssignmentResponse(
                userRole.getId(),
                userRole.getRole().getName(),
                userRole.getRole().getDescription(),
                userRole.getIsActive(),
                userRole.getCreatedAt(),
                userRole.getCreatedBy(),
                null // reason - would need to add to UserRole entity
        );
    }

    private UserStatsResponse getUserStats(Long userId) {
        // TODO: Implement actual queries to get real stats
        // For now, return mock data
        return new UserStatsResponse(
                5, // coursesEnrolled
                2, // coursesCompleted
                8, // eventsAttended
                12, // totalOrders
                BigDecimal.valueOf(450.00), // totalSpent
                BigDecimal.valueOf(200.00), // totalDonated
                40 // completionPercentage
        );
    }

    private List<RecentActivityResponse> getRecentActivities(Long userId) {
        // TODO: Implement actual activity tracking
        // For now, return mock data
        List<RecentActivityResponse> activities = new ArrayList<>();
        activities.add(new RecentActivityResponse(
                "COURSE_ENROLLMENT",
                "Biblical Leadership Course",
                "Enrolled in new course",
                LocalDateTime.now().minusDays(2),
                "course-123",
                "ACTIVE"));
        return activities;
    }

    private List<String> getRecommendations(User user) {
        // TODO: Implement recommendation engine
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Complete your Biblical Leadership course");
        recommendations.add("Register for upcoming Prayer Summit");
        recommendations.add("Update your profile information");
        return recommendations;
    }

    private String getCurrencyByRegion(String region) {
        return switch (region) {
            case "UK" -> "GBP";
            case "SOUTH_AFRICA" -> "ZAR";
            case "USA" -> "USD";
            case "GHANA" -> "GHS";
            case "NIGERIA" -> "NGN";
            default -> "USD";
        };
    }

    @Transactional(readOnly = true)
    public UserPlatformStatsResponse getPlatformUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long verifiedUsers = userRepository.countByIsEmailVerifiedTrue();
        long newUsersThisMonth = userRepository.countNewUsersFromDate(
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        long newUsersToday = userRepository.countNewUsersFromDate(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0));

        return new UserPlatformStatsResponse(
                totalUsers,
                activeUsers,
                verifiedUsers,
                newUsersThisMonth,
                newUsersToday,
                null,
                null,
                null,
                LocalDateTime.now());
    }

    public List<RecentActivityResponse> getUserActivity(int limit) {
        User currentUser = getCurrentUser();

        // TODO: Implement actual activity tracking queries
        // For now, return mock data
        List<RecentActivityResponse> activities = new ArrayList<>();

        activities.add(new RecentActivityResponse(
                "COURSE_ENROLLMENT",
                "Biblical Leadership Course",
                "Enrolled in new course about leadership principles",
                LocalDateTime.now().minusDays(1),
                "course-123",
                "ACTIVE"));

        activities.add(new RecentActivityResponse(
                "EVENT_REGISTRATION",
                "Prayer Summit 2024",
                "Registered for annual prayer summit",
                LocalDateTime.now().minusDays(3),
                "event-456",
                "CONFIRMED"));

        activities.add(new RecentActivityResponse(
                "PURCHASE",
                "Faith & Finance Book",
                "Purchased book on biblical financial principles",
                LocalDateTime.now().minusDays(5),
                "order-789",
                "COMPLETED"));

        activities.add(new RecentActivityResponse(
                "DONATION",
                "General Offering",
                "Made donation to general ministry fund",
                LocalDateTime.now().minusDays(7),
                "donation-012",
                "PROCESSED"));

        return activities.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
