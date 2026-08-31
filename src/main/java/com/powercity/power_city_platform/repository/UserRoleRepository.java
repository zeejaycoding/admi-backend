package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.Role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUser(User user);

    List<UserRole> findByRole(Role role);

    List<UserRole> findByRoleAndIsActiveTrue(Role role);

    Optional<UserRole> findByUserAndRole(User user, Role role);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.isActive = true")
    List<UserRole> findActiveRolesByUserId(@Param("userId") Long userId);

    boolean existsByUserAndRole(User user, Role role);

//    Additional

//    Optional<User> findByEmail(String email);
//    Optional<User> findByEmailVerificationToken(String token);
//    Optional<User> findByPasswordResetToken(String token);
//    boolean existsByEmail(String email);

//    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
//    Optional<User> findActiveUserByEmail(@Param("email") String email);

//    @Query("SELECT u FROM User u WHERE u.passwordResetToken = :token AND u.passwordResetTokenExpiry > :now")
//    Optional<User> findByValidPasswordResetToken(@Param("token") String token, @Param("now") LocalDateTime now);

//    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
//    long countNewUsersFromDate(@Param("startDate") LocalDateTime startDate);



    // NEW METHODS FOR USER MANAGEMENT

    // Search methods
//    @Query("SELECT u FROM User u WHERE " +
//            "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
//            "(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
//            "(:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
//            "(:region IS NULL OR u.region = :region) AND " +
//            "(:isActive IS NULL OR u.isActive = :isActive) AND " +
//            "(:isEmailVerified IS NULL OR u.isEmailVerified = :isEmailVerified)")
//    Page<User> findUsersWithFilters(
//            @Param("email") String email,
//            @Param("firstName") String firstName,
//            @Param("lastName") String lastName,
//            @Param("region") String region,
//            @Param("isActive") Boolean isActive,
//            @Param("isEmailVerified") Boolean isEmailVerified,
//            Pageable pageable
//    );
//
//    // Statistics methods
//    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
//    long countActiveUsers();
//
//    @Query("SELECT COUNT(u) FROM User u WHERE u.isEmailVerified = true")
//    long countVerifiedUsers();
//
//    @Query("SELECT u.region, COUNT(u) FROM User u GROUP BY u.region")
//    List<Object[]> countUsersByRegion();
//
//    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.createdAt < :endDate")
//    long countUsersCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
//
//    @Query("SELECT u FROM User u WHERE u.lastLogin >= :date")
//    List<User> findActiveUsersSince(@Param("date") LocalDateTime date);
//
//    // Find users by role
//    @Query("SELECT DISTINCT u FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = :roleName AND ur.isActive = true")
//    List<User> findUsersByRoleName(@Param("roleName") String roleName);
//
//    @Query("SELECT r.name, COUNT(DISTINCT u) FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE ur.isActive = true GROUP BY r.name")
//    List<Object[]> countUsersByRole();
//
//    // Find users without email verification
//    @Query("SELECT u FROM User u WHERE u.isEmailVerified = false AND u.createdAt < :cutoffDate")
//    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
//
//    // Find inactive users
//    @Query("SELECT u FROM User u WHERE u.lastLogin < :cutoffDate OR u.lastLogin IS NULL")
//    List<User> findInactiveUsersSince(@Param("cutoffDate") LocalDateTime cutoffDate);
//
//    // Performance queries
//    @Query("SELECT u FROM User u WHERE u.region = :region AND u.isActive = true")
//    Page<User> findActiveUsersByRegion(@Param("region") String region, Pageable pageable);
//
//    @Query("SELECT u FROM User u JOIN FETCH u.userRoles ur JOIN FETCH ur.role WHERE u.id = :userId")
//    Optional<User> findByIdWithRoles(@Param("userId") Long userId);
}
