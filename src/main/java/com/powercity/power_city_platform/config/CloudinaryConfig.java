package com.powercity.power_city_platform.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudinary Configuration
 *
 * Configures Cloudinary client for image and video storage/optimization.
 * Used for: book covers, user avatars, event images, course videos, etc.
 *
 * PDFs and audio files still use AWS S3 (see S3Config.java)
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.secure:true}")
    private Boolean secure;

    /**
     * Creates Cloudinary bean for dependency injection
     *
     * @return Configured Cloudinary instance
     */
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", secure
        ));
    }
}
