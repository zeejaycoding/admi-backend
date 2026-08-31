package com.powercity.power_city_platform.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Cloudinary Service
 *
 * Handles all Cloudinary operations for image and video storage/optimization.
 *
 * Responsibilities:
 * - Book covers (front & back) → Auto-optimized, WebP/AVIF conversion
 * - User profile images → Face detection, thumbnails
 * - Event/Campus images → Responsive images
 * - Course thumbnails → Auto-optimization
 * - Course videos → HLS streaming, adaptive bitrate
 *
 * PDFs and audio files use S3FileService (not this service)
 */
@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private FileSecurityService fileSecurityService;

    @org.springframework.beans.factory.annotation.Value("${cloudinary.folder-prefix:prod/}")
    private String folderPrefix;

    /**
     * Build full Cloudinary folder path with environment prefix
     * e.g., "dev/books/covers" or "prod/courses/thumbnails"
     */
    private String buildFolderPath(String relativeFolder) {
        // Remove trailing slash from prefix if it exists, and ensure proper path concatenation
        String prefix = folderPrefix.endsWith("/") ? folderPrefix.substring(0, folderPrefix.length() - 1) : folderPrefix;
        return prefix + "/" + relativeFolder;
    }

    /**
     * Upload book cover image (optimized for fast loading)
     *
     * @param file   The cover image file
     * @param bookId The book ID
     * @return Map with "url" and "publicId"
     * @throws IOException If upload fails
     */
    public Map<String, String> uploadBookCover(MultipartFile file, Long bookId) throws IOException {
        // Security validation
        FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                FileSecurityService.FileType.IMAGE);
        if (!validation.isValid()) {
            throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
        }

        log.info("Uploading book cover for book ID: {}", bookId);

        try {
            // Upload with optimizations
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", buildFolderPath("books/covers"),
                    "public_id", "book-" + bookId + "-cover-" + UUID.randomUUID(),
                    "transformation", new Transformation()
                        .width(600).height(900).crop("limit")  // Max dimensions (2:3 aspect ratio)
                        .quality("auto")                         // Auto quality optimization
                        .fetchFormat("auto"),                    // Auto format (WebP/AVIF for modern browsers)
                    "resource_type", "image"
                )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Successfully uploaded book cover. URL: {}", url);

            return Map.of(
                "url", url,
                "publicId", publicId
            );

        } catch (IOException e) {
            log.error("Failed to upload book cover for book ID {}: {}", bookId, e.getMessage());
            throw new RuntimeException("Failed to upload book cover: " + e.getMessage(), e);
        }
    }

    /**
     * Upload book back cover
     *
     * @param file   The back cover image file
     * @param bookId The book ID
     * @return Map with "url" and "publicId"
     * @throws IOException If upload fails
     */
    public Map<String, String> uploadBookBackCover(MultipartFile file, Long bookId) throws IOException {
        // Security validation
        FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                FileSecurityService.FileType.IMAGE);
        if (!validation.isValid()) {
            throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
        }

        log.info("Uploading book back cover for book ID: {}", bookId);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", buildFolderPath("books/back-covers"),
                    "public_id", "book-" + bookId + "-back-cover-" + UUID.randomUUID(),
                    "transformation", new Transformation()
                        .width(600).height(900).crop("limit")
                        .quality("auto")
                        .fetchFormat("auto"),
                    "resource_type", "image"
                )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Successfully uploaded book back cover. URL: {}", url);

            return Map.of(
                "url", url,
                "publicId", publicId
            );

        } catch (IOException e) {
            log.error("Failed to upload book back cover for book ID {}: {}", bookId, e.getMessage());
            throw new RuntimeException("Failed to upload book back cover: " + e.getMessage(), e);
        }
    }

    /**
     * Upload profile image with thumbnail generation
     *
     * @param file   The profile image file
     * @param userId The user ID
     * @return Map with "url", "thumbnailUrl", and "publicId"
     * @throws IOException If upload fails
     */
    public Map<String, String> uploadProfileImage(MultipartFile file, Long userId) throws IOException {
        // Security validation
        FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                FileSecurityService.FileType.IMAGE);
        if (!validation.isValid()) {
            throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
        }

        log.info("Uploading profile image for user ID: {}", userId);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", buildFolderPath("profiles/avatars"),
                    "public_id", "user-" + userId + "-avatar-" + UUID.randomUUID(),
                    "transformation", new Transformation()
                        .width(300).height(300).crop("fill").gravity("face")  // Face detection for smart cropping
                        .quality("auto")
                        .fetchFormat("auto"),
                    "eager", Arrays.asList(
                        // Generate thumbnail eagerly (immediately)
                        new Transformation()
                            .width(100).height(100).crop("fill").gravity("face")
                            .quality("auto")
                            .fetchFormat("auto")
                    ),
                    "resource_type", "image"
                )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            // Get thumbnail URL from eager transformations
            List eagerList = (List) uploadResult.get("eager");
            String thumbnailUrl = eagerList != null && !eagerList.isEmpty()
                ? (String) ((Map) eagerList.get(0)).get("secure_url")
                : url; // Fallback to main URL if thumbnail generation failed

            log.info("Successfully uploaded profile image with thumbnail. URL: {}", url);

            return Map.of(
                "url", url,
                "thumbnailUrl", thumbnailUrl,
                "publicId", publicId
            );

        } catch (IOException e) {
            log.error("Failed to upload profile image for user ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to upload profile image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload event or campus image (larger, optimized for hero sections)
     *
     * @param file   The image file
     * @param folder Cloudinary folder (e.g., "events/images" or "campuses/images")
     * @param prefix Filename prefix (e.g., "event-123" or "campus-456")
     * @return Map with "url" and "publicId"
     * @throws IOException If upload fails
     */
    public Map<String, String> uploadEventOrCampusImage(MultipartFile file, String folder, String prefix) throws IOException {
        // Security validation
        FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                FileSecurityService.FileType.IMAGE);
        if (!validation.isValid()) {
            throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
        }

        log.info("Uploading image to folder: {}, prefix: {}", folder, prefix);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", buildFolderPath(folder),  // e.g., "dev/events/images" or "prod/campuses/images"
                    "public_id", prefix + "-" + UUID.randomUUID(),
                    "transformation", new Transformation()
                        .width(1200).height(800).crop("limit")  // Larger for hero images
                        .quality("auto:good")                     // Higher quality for hero sections
                        .fetchFormat("auto"),
                    "resource_type", "image"
                )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Successfully uploaded image. URL: {}", url);

            return Map.of(
                "url", url,
                "publicId", publicId
            );

        } catch (IOException e) {
            log.error("Failed to upload image to {}/{}: {}", folder, prefix, e.getMessage());
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload course thumbnail
     *
     * @param file     The thumbnail image file
     * @param courseId The course ID
     * @return Map with "url" and "publicId"
     * @throws IOException If upload fails
     */
    public Map<String, String> uploadCourseThumbnail(MultipartFile file, Long courseId) throws IOException {
        // Security validation
        FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                FileSecurityService.FileType.IMAGE);
        if (!validation.isValid()) {
            throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
        }

        log.info("Uploading course thumbnail for course ID: {}", courseId);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", buildFolderPath("courses/thumbnails"),
                    "public_id", "course-" + courseId + "-thumbnail-" + UUID.randomUUID(),
                    "transformation", new Transformation()
                        .width(800).height(450).crop("fill")  // 16:9 aspect ratio
                        .quality("auto")
                        .fetchFormat("auto"),
                    "resource_type", "image"
                )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Successfully uploaded course thumbnail. URL: {}", url);

            return Map.of(
                "url", url,
                "publicId", publicId
            );

        } catch (IOException e) {
            log.error("Failed to upload course thumbnail for course ID {}: {}", courseId, e.getMessage());
            throw new RuntimeException("Failed to upload course thumbnail: " + e.getMessage(), e);
        }
    }

    /**
     * Upload course video lesson (streaming optimized)
     *
     * @param file     The video file
     * @param courseId The course ID
     * @param lessonId The lesson ID
     * @return Map with "url", "publicId", and "duration" (in seconds)
     * @throws IOException If upload fails
     */
    public Map<String, String> uploadCourseVideo(MultipartFile file, Long courseId, Long lessonId) throws IOException {
        log.info("Uploading course video for course ID: {}, lesson ID: {}", courseId, lessonId);

        try {
            // Video uploads can be large, Cloudinary handles them well
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", buildFolderPath("courses/videos"),
                    "public_id", "course-" + courseId + "-lesson-" + lessonId + "-" + UUID.randomUUID(),
                    "resource_type", "video",
                    // HLS streaming is available in Cloudinary but may require a paid plan
                    // For now, we'll just upload the video and Cloudinary will optimize it
                    "eager_async", true,  // Process HLS in background (faster upload response)
                    "transformation", new Transformation()
                        .quality("auto")
                        .fetchFormat("auto")
                )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            Object durationObj = uploadResult.get("duration");
            String duration = durationObj != null ? String.valueOf(durationObj) : "0";

            log.info("Successfully uploaded course video. URL: {}, Duration: {}s", url, duration);

            return Map.of(
                "url", url,
                "publicId", publicId,
                "duration", duration  // Duration in seconds
            );

        } catch (IOException e) {
            log.error("Failed to upload course video for course ID {}, lesson ID {}: {}", courseId, lessonId, e.getMessage());
            throw new RuntimeException("Failed to upload course video: " + e.getMessage(), e);
        }
    }

    /**
     * Upload course promo video
     *
     * @param file     The promo video file
     * @param courseId The course ID
     * @return Map with "url", "publicId", and "thumbnailUrl" (auto-generated from video)
     * @throws IOException If upload fails
     */
    public Map<String, String> uploadPromoVideo(MultipartFile file, Long courseId) throws IOException {
        log.info("Uploading course promo video for course ID: {}", courseId);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", buildFolderPath("courses/promos"),
                    "public_id", "course-" + courseId + "-promo-" + UUID.randomUUID(),
                    "resource_type", "video",
                    "transformation", new Transformation()
                        .quality("auto")
                        .fetchFormat("auto"),
                    "eager", Arrays.asList(
                        // Generate thumbnail from video (auto-select best frame)
                        new Transformation()
                            .width(800).height(450).crop("fill")
                            .startOffset("auto")  // Auto-select best frame
                    )
                )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            // Get thumbnail URL from eager transformations
            List eagerList = (List) uploadResult.get("eager");
            String thumbnailUrl = eagerList != null && !eagerList.isEmpty()
                ? (String) ((Map) eagerList.get(0)).get("secure_url")
                : url;

            log.info("Successfully uploaded promo video with thumbnail. URL: {}", url);

            return Map.of(
                "url", url,
                "publicId", publicId,
                "thumbnailUrl", thumbnailUrl
            );

        } catch (IOException e) {
            log.error("Failed to upload promo video for course ID {}: {}", courseId, e.getMessage());
            throw new RuntimeException("Failed to upload promo video: " + e.getMessage(), e);
        }
    }

    /**
     * Delete file from Cloudinary
     *
     * @param publicId     The Cloudinary public ID
     * @param resourceType The resource type ("image" or "video")
     * @throws IOException If deletion fails
     */
    public void deleteFile(String publicId, String resourceType) throws IOException {
        if (publicId == null || publicId.trim().isEmpty()) {
            log.warn("Attempted to delete file with null or empty publicId");
            return;
        }

        log.info("Deleting file from Cloudinary: {} (type: {})", publicId, resourceType);

        try {
            cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", resourceType)
            );

            log.info("Successfully deleted file: {}", publicId);

        } catch (IOException e) {
            log.error("Failed to delete file {} from Cloudinary: {}", publicId, e.getMessage());
            throw new RuntimeException("Failed to delete file from Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Generate responsive image URL with transformations
     * (Used in frontend to dynamically resize images)
     *
     * @param publicId The Cloudinary public ID
     * @param width    Desired width
     * @param height   Desired height
     * @return Transformed image URL
     */
    public String generateResponsiveUrl(String publicId, int width, int height) {
        return cloudinary.url()
            .transformation(new Transformation()
                .width(width).height(height).crop("fill")
                .quality("auto")
                .fetchFormat("auto")
            )
            .generate(publicId);
    }

    /**
     * Generate video URL (optimized for streaming)
     *
     * @param publicId The Cloudinary video public ID
     * @return Video URL
     */
    public String generateVideoStreamingUrl(String publicId) {
        return cloudinary.url()
            .resourceType("video")
            .secure(true)
            .generate(publicId);
    }

    /**
     * Get the base URL for a Cloudinary file
     *
     * @param publicId The Cloudinary public ID
     * @return Full HTTPS URL
     */
    public String getFileUrl(String publicId) {
        return cloudinary.url().secure(true).generate(publicId);
    }
}
