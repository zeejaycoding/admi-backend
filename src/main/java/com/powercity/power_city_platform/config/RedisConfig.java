package com.powercity.power_city_platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Profile("redis") // Only active when redis profile is enabled
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);

        if (!redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        return new JedisConnectionFactory(redisConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        // Configure cache-specific TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // User profile cache (30 minutes)
        cacheConfigurations.put("userProfile", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));

        // User dashboard cache (10 minutes)
        cacheConfigurations.put("userDashboard", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));

        // User roles cache (1 hour)
        cacheConfigurations.put("userRoles", defaultCacheConfig.entryTtl(Duration.ofHours(1)));

        // User stats cache (15 minutes)
        cacheConfigurations.put("userStats", defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));

        // JWT token blacklist cache (24 hours)
        cacheConfigurations.put("jwtBlacklist", defaultCacheConfig.entryTtl(Duration.ofHours(24)));

        // Session cache (2 hours)
        cacheConfigurations.put("userSession", defaultCacheConfig.entryTtl(Duration.ofHours(2)));

        // Regional data cache (1 hour)
        cacheConfigurations.put("regionalData", defaultCacheConfig.entryTtl(Duration.ofHours(1)));

        // Course data cache (30 minutes)
        cacheConfigurations.put("courseData", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));

        // Event data cache (15 minutes)
        cacheConfigurations.put("eventData", defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

}
