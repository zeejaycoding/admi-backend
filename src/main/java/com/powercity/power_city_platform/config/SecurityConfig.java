package com.powercity.power_city_platform.config;

import com.powercity.power_city_platform.security.JwtAuthenticationEntryPoint;
import com.powercity.power_city_platform.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/api/v1/payments/session/**").permitAll()

                        // Donation public endpoints
                        .requestMatchers("/api/v1/donations/create").permitAll()
                        .requestMatchers("/api/v1/donations/capture/**").permitAll()
                        .requestMatchers("/api/v1/donations/webhook/**").permitAll()
                        .requestMatchers("/api/v1/donations/{id}").permitAll()
                        .requestMatchers("/api/v1/donations/order/**").permitAll()
                        .requestMatchers("/api/v1/donations/email/**").permitAll()

                        // Campus public endpoints
                        .requestMatchers("/api/v1/campuses/all").permitAll()
                        .requestMatchers("/api/v1/campuses/search").permitAll()
                        .requestMatchers("/api/v1/campuses/regions").permitAll()
                        .requestMatchers("/api/v1/campuses/currencies").permitAll()
                        .requestMatchers("/api/v1/campuses/currency/**").permitAll()
                        .requestMatchers("/api/v1/campuses/public/**").permitAll()
                        .requestMatchers("/api/v1/campuses/*/analytics").permitAll()
                        .requestMatchers("/api/v1/campuses/analytics/**").permitAll()
                        .requestMatchers("/api/v1/campuses/{campusId}").permitAll()

                        // Course public endpoints (for browsing E-Store)
                        .requestMatchers("/api/v1/courses").permitAll()
                        .requestMatchers("/api/v1/courses/{courseId}").permitAll()
                        .requestMatchers("/api/v1/courses/featured").permitAll()
                        .requestMatchers("/api/v1/courses/recent").permitAll()
                        .requestMatchers("/api/v1/courses/top-enrolled").permitAll()
                        .requestMatchers("/api/v1/courses/category/**").permitAll()
                        .requestMatchers("/api/v1/courses/search").permitAll()

                        // Book public endpoints (for browsing E-Store)
                        .requestMatchers("/api/v1/books").permitAll()
                        .requestMatchers("/api/v1/books/{bookId}").permitAll()
                        .requestMatchers("/api/v1/books/featured").permitAll()
                        .requestMatchers("/api/v1/books/recent").permitAll()
                        .requestMatchers("/api/v1/books/search").permitAll()

                        // Event public endpoints (for the home-page events ticker); writes are still guarded by @PreAuthorize
                        .requestMatchers("/api/v1/events").permitAll()
                        .requestMatchers("/api/v1/events/{id}").permitAll()
                        .requestMatchers("/api/v1/events/search").permitAll()

                        // Form public endpoints (for viewing/submitting forms)
                        .requestMatchers("/api/v1/forms").permitAll()
                        .requestMatchers("/api/v1/forms/paginated").permitAll()
                        .requestMatchers("/api/v1/forms/{formId}/submit").permitAll()
                        .requestMatchers("/api/v1/forms/code/{formCode}").permitAll()
                        .requestMatchers("/api/v1/forms/event/{eventCode}").permitAll()
                        .requestMatchers("/api/v1/forms/{formId}/checkout/stripe").permitAll()
                        .requestMatchers("/api/v1/forms/{formId}/checkout/paypal").permitAll()
                        .requestMatchers("/api/v1/forms/{formId}/submit/bank-transfer").permitAll()
                        .requestMatchers("/api/v1/forms/submission/by-session/**").permitAll()
                        .requestMatchers("/api/v1/form-submissions/{formId}").permitAll()
                        .requestMatchers("/api/v1/contact").permitAll()

                        // Swagger/OpenAPI endpoints - comprehensive patterns
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/swagger-ui/**").permitAll()
                        .requestMatchers("/api/v1/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/v3/api-docs.yaml").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/swagger-config/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()

                        // Actuator endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Coordinator chat websocket (auth handled in websocket handshake interceptor)
                        .requestMatchers("/ws/**").permitAll()

                        // Static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("SUPER_ADMIN")

                        // Protected endpoints
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}