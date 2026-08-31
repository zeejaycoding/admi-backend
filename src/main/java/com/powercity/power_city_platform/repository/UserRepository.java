package com.powercity.power_city_platform.repository;
import com.powercity.power_city_platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role " +
            "WHERE u.isActive = true AND ur.isActive = true AND ur.role.name = 'COORDINATOR' " +
            "AND ((:country = '' OR :country IS NULL) OR LOWER(u.region) = LOWER(CAST(:country AS string))) " +
            "AND ((:searchTerm = '' OR :searchTerm IS NULL) OR " +
            "     LOWER(u.fullName) LIKE LOWER(CAST(CONCAT('%', :searchTerm, '%') AS string)) " +
            "     OR LOWER(u.email) LIKE LOWER(CAST(CONCAT('%', :searchTerm, '%') AS string)))")
    List<User> findCoordinators(@Param("country") String country, @Param("searchTerm") String searchTerm);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.email = :email AND u.isActive = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.passwordResetToken = :token AND u.passwordResetTokenExpiry > :now")
    Optional<User> findByValidPasswordResetToken(@Param("token") String token, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countNewUsersFromDate(@Param("startDate") LocalDateTime startDate);

    Long countByIsActiveTrue();
    Long countByIsActiveFalse();
    Long countByIsEmailVerifiedTrue();
}