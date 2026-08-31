package com.powercity.power_city_platform.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

/**
 * Storage for course audio. Uses Cloudflare R2 (zero egress) when configured, otherwise
 * falls back to the shared S3 storage. Only course audio is routed here — books, profiles
 * and documents continue to use {@link S3FileService} directly.
 */
@Service
public class CourseAudioStorageService {

    private static final Logger log = LoggerFactory.getLogger(CourseAudioStorageService.class);
    private static final String COURSE_AUDIO_PREFIX = "courses/audio/";

    @Value("${r2.endpoint:}")
    private String endpoint;
    @Value("${r2.access-key:}")
    private String accessKey;
    @Value("${r2.secret-key:}")
    private String secretKey;
    @Value("${r2.bucket-name:}")
    private String bucket;
    @Value("${r2.region:auto}")
    private String region;

    // Reuse the same env folder prefix as S3 (dev/ or prod/) so a shared R2 bucket stays split by environment.
    @Value("${aws.s3.folder-prefix:prod/}")
    private String folderPrefix;

    private final S3FileService s3FileService; // fallback when R2 is not configured

    private S3Client r2Client;
    private S3Presigner r2Presigner;

    public CourseAudioStorageService(S3FileService s3FileService) {
        this.s3FileService = s3FileService;
    }

    @PostConstruct
    void init() {
        if (isBlank(endpoint) || isBlank(accessKey) || isBlank(secretKey) || isBlank(bucket)) {
            log.info("R2 not configured — course audio will use S3 storage.");
            return;
        }
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        var s3Config = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        r2Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config)
                .httpClient(ApacheHttpClient.builder()
                        .connectionTimeout(Duration.ofMinutes(5))
                        .socketTimeout(Duration.ofMinutes(10))
                        .build())
                .build();

        r2Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config)
                .build();

        log.info("Course audio storage: using Cloudflare R2 bucket '{}'", bucket);
    }

    public boolean isR2Enabled() {
        return r2Client != null;
    }

    /** Upload a course audio file, returning its storage key. */
    public String uploadCourseAudio(Long courseId, MultipartFile file) {
        if (!isR2Enabled()) {
            return s3FileService.uploadCourseAudio(courseId, file);
        }
        try {
            // Keep the real, human-readable filename so the bucket is manageable — foldered per course,
            // with a numeric suffix added only if that exact name is already taken.
            String ext = fileExtension(file.getOriginalFilename());
            String base = slugify(baseName(file.getOriginalFilename()));
            if (base.isEmpty()) base = "lesson";
            String folder = normalizedPrefix() + COURSE_AUDIO_PREFIX + "course-" + courseId + "/";
            String key = uniqueKey(folder, base, ext);
            r2Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload course audio to R2: " + e.getMessage(), e);
        }
    }

    /** Presigned GET URL for streaming a course audio object. */
    public String presignedUrl(String key, Duration ttl) {
        if (!isR2Enabled()) {
            return s3FileService.generatePresignedUrl(key, ttl);
        }
        var presigned = r2Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build());
        return presigned.url().toString();
    }

    /** Delete a course audio object. */
    public void delete(String key) {
        if (!isR2Enabled()) {
            s3FileService.deleteFile(key);
            return;
        }
        r2Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @PreDestroy
    void close() {
        if (r2Client != null) r2Client.close();
        if (r2Presigner != null) r2Presigner.close();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Env folder prefix (e.g. "dev/" or "prod/"), normalized to end with a single slash, or empty. */
    private String normalizedPrefix() {
        if (isBlank(folderPrefix)) return "";
        String p = folderPrefix.trim();
        return p.endsWith("/") ? p : p + "/";
    }

    private static String fileExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }

    /** The filename without its path or extension. */
    private static String baseName(String filename) {
        if (filename == null) return "";
        String name = filename;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) name = name.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Make a filename safe for an object key: keep letters/digits/._-, spaces become dashes. */
    private static String slugify(String value) {
        if (value == null) return "";
        return value.trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9._-]", "")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    /** Return {folder}{base}.{ext}, adding -2, -3… before the extension if that key already exists. */
    private String uniqueKey(String folder, String base, String ext) {
        String suffix = ext.isEmpty() ? "" : "." + ext;
        String candidate = folder + base + suffix;
        int n = 2;
        while (objectExists(candidate)) {
            candidate = folder + base + "-" + n + suffix;
            n++;
        }
        return candidate;
    }

    private boolean objectExists(String key) {
        try {
            r2Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }
}
