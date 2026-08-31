package com.powercity.power_city_platform.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
@Profile("!redis") // Only active when redis profile is NOT active
public class SimpleCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Simple in-memory cache manager
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        // Define cache names (same as Redis config)
        cacheManager.setCacheNames(java.util.List.of(
                "userProfile",
                "userDashboard",
                "userRoles",
                "userStats",
                "jwtBlacklist",
                "userSession",
                "regionalData",
                "courseData",
                "eventData"));

        return cacheManager;
    }
}