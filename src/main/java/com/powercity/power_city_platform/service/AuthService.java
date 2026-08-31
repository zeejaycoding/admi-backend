package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.auth.*;
import com.powercity.power_city_platform.event.UserRegisteredEvent;
import com.powercity.power_city_platform.dto.response.auth.AuthResponse;
import com.powercity.power_city_platform.dto.response.auth.TokenResponse;
import com.powercity.power_city_platform.dto.response.auth.UserInfo;
import com.powercity.power_city_platform.entity.Role;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.RoleRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import com.powercity.power_city_platform.repository.UserRoleRepository;
import com.powercity.power_city_platform.service.email.WelcomeEmailService;
import com.powercity.power_city_platform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;
    private final WelcomeEmailService welcomeEmailService;
    private final PermissionService permissionService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // Constants for rate limiting
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("User with this email already exists");
        }

        // Create new user
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.region()
        );

        user.setCurrency(getCurrencyByRegion(request.region()));
        user.setEmailVerificationToken(UUID.randomUUID().toString());

        // Save user
        User savedUser = userRepository.save(user);

        // Assign default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default USER role not found"));

        UserRole userRoleAssignment = new UserRole(savedUser, userRole);
        userRoleRepository.save(userRoleAssignment);

        UserInfo userInfo = createUserInfo(savedUser);

        // Send the verification email only after this transaction commits.
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser, savedUser.getEmailVerificationToken()));

        // Verify-before-login: no session/tokens are issued at registration.
        return new AuthResponse(null, null, null, null, userInfo);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            throw new LockedException("Account is temporarily locked. Please try again later.");
        }

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled. Please verify your email.");
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // Reset failed login attempts on successful login
            user.resetFailedLoginAttempts();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Create user info
            UserInfo userInfo = createUserInfo(user);

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    jwtUtil.getExpirationTime(),
                    userInfo
            );

        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            user.incrementFailedLoginAttempts();

            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lockAccount(LOCKOUT_DURATION_MINUTES);
            }

            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public void initiatePasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user != null) {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1)); // 1 hour expiry
            userRepository.save(user);

            // Eagerly initialize User entity to avoid LazyInitializationException in async method
            String email = user.getEmail();
            String firstName = user.getFirstName();

            // Send password reset email
            try {
                emailService.sendPasswordResetEmail(
                        email,
                        firstName,
                        resetToken
                );
            } catch (Exception e) {
                logger.error("Failed to send password reset email: {}", e.getMessage(), e);
            }
        }

        // Always return success for security (don't reveal if email exists)
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByValidPasswordResetToken(
                request.token(),
                LocalDateTime.now()
        ).orElseThrow(() -> new ResourceNotFoundException("Invalid or expired password reset token"));

        // Update password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.resetFailedLoginAttempts(); // Reset any lockout

        userRepository.save(user);
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Token is not a refresh token");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtUtil.generateToken(userDetails);
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        return new TokenResponse(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getExpirationTime()
        );
    }

    public void logout(String token) {
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email verification token"));



        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
    }

    public void resendEmailVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        // Generate new verification token if needed
        if (user.getEmailVerificationToken() == null) {
            user.setEmailVerificationToken(UUID.randomUUID().toString());
            userRepository.save(user);
        }

        // Eagerly initialize User entity to avoid LazyInitializationException in async method
        user.getFirstName();
        user.getRegion();

        // Resend verification email
        try {
            welcomeEmailService.sendEmailVerification(user, user.getEmailVerificationToken());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

    private UserInfo createUserInfo(User user) {
        Set<String> roles = new HashSet<>();

        // Safe handling of null userRoles
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            roles = user.getUserRoles().stream()
                    .filter(userRole -> userRole != null && userRole.getIsActive())
                    .map(userRole -> userRole.getRole().getName())
                    .collect(Collectors.toSet());
        } else {
            // Default role if no roles found
            roles.add("USER");
        }

        Set<String> permissions = permissionService.getEffectivePermissions(roles);

        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRegion(),
                user.getCurrency(),
                user.getIsEmailVerified(),
                roles,
                permissions,
                user.getLastLogin()
        );
    }

    private String getCurrencyByRegion(String region) {
        if (region == null) {
            return "USD";
        }
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
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }
}
