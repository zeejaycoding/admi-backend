package com.powercity.power_city_platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

@Configuration
public class S3Config {

    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

    @Value("${aws.s3.access-key:${AWS_ACCESS_KEY_ID:}}")
    private String accessKey;

    @Value("${aws.s3.secret-key:${AWS_SECRET_ACCESS_KEY:}}")
    private String secretKey;

    @Value("${aws.s3.region:${AWS_DEFAULT_REGION:us-east-1}}")
    private String region;

    @Bean
    public S3Client s3Client() {
        logger.info("Configuring S3Client with region: {}", region);
        logger.info("Access key configured: {}", !accessKey.isEmpty());
        logger.info("Secret key configured: {}", !secretKey.isEmpty());

        // Configure HTTP client with increased timeouts for large file uploads
        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .connectionTimeout(Duration.ofMinutes(5))
                .socketTimeout(Duration.ofMinutes(10))
                .build();

        // Configure client with increased API call timeout
        ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMinutes(15))
                .apiCallAttemptTimeout(Duration.ofMinutes(10))
                .build();

        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            logger.warn("AWS credentials not found in application properties. Using default credentials chain.");
            // Use default credentials chain (IAM roles, environment variables, etc.)
            return S3Client.builder()
                    .region(Region.of(region))
                    .httpClient(httpClient)
                    .overrideConfiguration(clientConfig)
                    .build();
        } else {
            logger.info("Using explicit AWS credentials from application properties");
            // Use explicit credentials
            AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .httpClient(httpClient)
                    .overrideConfiguration(clientConfig)
                    .build();
        }
    }

    @Bean
    public S3Presigner s3Presigner() {
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            logger.warn("Using default credentials chain for S3Presigner");
            // Use default credentials chain
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .build();
        } else {
            logger.info("Using explicit AWS credentials for S3Presigner");
            // Use explicit credentials
            AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        }
    }
}
