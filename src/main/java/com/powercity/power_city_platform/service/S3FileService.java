package com.powercity.power_city_platform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class S3FileService {

    private static final Logger log = LoggerFactory.getLogger(S3FileService.class);

    @Autowired(required = false)
    private S3Client s3Client;

    @Autowired(required = false)
    private S3Presigner s3Presigner;

    @Autowired
    private FileSecurityService fileSecurityService;

    @Value("${aws.s3.bucket-name:powercity-platform-files}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.folder-prefix:prod/}")
    private String folderPrefix;

    @Value("${app.local-upload-dir:./uploads}")
    private String localUploadDir;

    private boolean s3Available = true;

    // Updated folder structure constants (without environment prefix)
    private static final String PROFILE_IMAGES_PREFIX = "profiles/images/";
    private static final String PROFILE_DOCUMENTS_PREFIX = "profiles/documents/";
    private static final String COURSE_AUDIO_PREFIX = "courses/audio/";
    private static final String COURSE_MATERIALS_PREFIX = "courses/materials/";
    private static final String BOOK_PDF_PREFIX = "books/pdf/";
    private static final String BOOK_COVERS_PREFIX = "books/covers/";
    private static final String BOOK_BACK_COVERS_PREFIX = "books/back-covers/";
    private static final String EVENT_IMAGES_PREFIX = "events/images/";
    private static final String EVENT_DOCUMENTS_PREFIX = "events/documents/";
    private static final String CAMPUS_IMAGES_PREFIX = "campuses/images/";
    private static final String CAMPUS_BANNERS_PREFIX = "campuses/banners/";
    private static final String TEMP_UPLOADS_PREFIX = "temp/uploads/";

    // Legacy support (deprecated)
    private static final String EMAIL_ASSETS_PREFIX = "email-assets/";

    @jakarta.annotation.PostConstruct
    public void init() {
        if (s3Client == null) {
            log.warn("S3Client not available — all file operations will use local storage at: {}", localUploadDir);
            s3Available = false;
        }
        try {
            java.io.File dir = new java.io.File(localUploadDir);
            if (!dir.exists()) dir.mkdirs();
        } catch (Exception e) {
            log.warn("Could not create local upload directory: {}", localUploadDir);
        }
    }

    // Profile image dimensions for optimization
    private static final int PROFILE_IMAGE_SIZE = 300;
    private static final int THUMBNAIL_SIZE = 100;

    /**
     * Upload profile image for a user
     */
    public String uploadProfileImage(Long userId, MultipartFile file) {
        try {
            // Security validation
            FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                    FileSecurityService.FileType.IMAGE);
            if (!validation.isValid()) {
                throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "user-" + userId + "-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(PROFILE_IMAGES_PREFIX + filename);

            // Optimize image
            BufferedImage optimizedImage = optimizeImage(file, PROFILE_IMAGE_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(optimizedImage, fileExtension, baos);
            byte[] imageBytes = baos.toByteArray();

            // Upload to S3
            uploadToS3(key, imageBytes, file.getContentType());

            // Generate and upload thumbnail
            BufferedImage thumbnail = optimizeImage(file, THUMBNAIL_SIZE);
            ByteArrayOutputStream thumbBaos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, fileExtension, thumbBaos);
            byte[] thumbBytes = thumbBaos.toByteArray();

            String thumbKey = buildS3Key(PROFILE_IMAGES_PREFIX + "thumbnails/" + filename);
            uploadToS3(thumbKey, thumbBytes, file.getContentType());

            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload course audio file (streaming for large files)
     */
    public String uploadCourseAudio(Long courseId, MultipartFile file) {
        try {
            // Security validation
            FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                    FileSecurityService.FileType.AUDIO);
            if (!validation.isValid()) {
                throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "course-" + courseId + "-audio-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(COURSE_AUDIO_PREFIX + filename);

            // Stream file to S3 instead of loading into memory (for large audio files)
            uploadToS3Stream(key, file.getInputStream(), file.getSize(), file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload course audio: " + e.getMessage(), e);
        }
    }

    /**
     * Upload course material file (streaming for large files)
     */
    public String uploadCourseMaterial(Long courseId, MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "course-" + courseId + "-material-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(COURSE_MATERIALS_PREFIX + filename);

            // Stream file to S3 instead of loading into memory
            uploadToS3Stream(key, file.getInputStream(), file.getSize(), file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload course material: " + e.getMessage(), e);
        }
    }

    /**
     * Upload course content file (backward compatibility)
     * 
     * @deprecated Use uploadCourseAudio or uploadCourseMaterial instead
     */
    @Deprecated
    public String uploadCourseContent(Long courseId, MultipartFile file, String contentType) {
        if ("audio".equalsIgnoreCase(contentType)) {
            return uploadCourseAudio(courseId, file);
        } else {
            return uploadCourseMaterial(courseId, file);
        }
    }

    /**
     * Upload book PDF file (streaming for large files)
     */
    public String uploadBookPdf(Long bookId, MultipartFile file) {
        try {
            System.out.println("uploadBookPdf - Starting validation for book " + bookId);

            // Security validation
            FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                    FileSecurityService.FileType.PDF);
            if (!validation.isValid()) {
                throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
            }

            System.out.println("uploadBookPdf - Validation passed, generating filename");
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "book-" + bookId + "-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(BOOK_PDF_PREFIX + filename);

            System.out.println("uploadBookPdf - Calling uploadToS3Stream with key: " + key);
            uploadToS3Stream(key, file.getInputStream(), file.getSize(), file.getContentType());
            System.out.println("uploadBookPdf - Upload completed successfully");
            return key;
        } catch (Exception e) {
            System.err.println("uploadBookPdf - FAILED: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload book PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Upload book cover image
     */
    public String uploadBookCover(Long bookId, MultipartFile file) {
        try {
            // Security validation
            FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                    FileSecurityService.FileType.IMAGE);
            if (!validation.isValid()) {
                throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "book-" + bookId + "-cover-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(BOOK_COVERS_PREFIX + filename);

            // Optimize image for cover
            BufferedImage optimizedImage = optimizeImage(file, 600); // Larger size for book covers
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(optimizedImage, fileExtension, baos);
            byte[] imageBytes = baos.toByteArray();

            uploadToS3(key, imageBytes, file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload book cover: " + e.getMessage(), e);
        }
    }

    /**
     * Upload book back cover image
     */
    public String uploadBookBackCover(Long bookId, MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "book-" + bookId + "-back-cover-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(BOOK_BACK_COVERS_PREFIX + filename);

            // Optimize image for back cover
            BufferedImage optimizedImage = optimizeImage(file, 600); // Larger size for book covers
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(optimizedImage, fileExtension, baos);
            byte[] imageBytes = baos.toByteArray();

            uploadToS3(key, imageBytes, file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload book back cover: " + e.getMessage(), e);
        }
    }

    /**
     * Upload event image
     */
    public String uploadEventImage(Long eventId, MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "event-" + eventId + "-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(EVENT_IMAGES_PREFIX + filename);

            // Optimize image for events
            BufferedImage optimizedImage = optimizeImage(file, 800); // Medium size for event images
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(optimizedImage, fileExtension, baos);
            byte[] imageBytes = baos.toByteArray();

            uploadToS3(key, imageBytes, file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload event image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload campus image
     */
    public String uploadCampusImage(Long campusId, MultipartFile file) {
        try {
            // Security validation
            FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                    FileSecurityService.FileType.IMAGE);
            if (!validation.isValid()) {
                throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "campus-" + campusId + "-image-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(CAMPUS_IMAGES_PREFIX + filename);

            // Optimize image for campus
            BufferedImage optimizedImage = optimizeImage(file, 800); // Medium size for campus images
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(optimizedImage, fileExtension, baos);
            byte[] imageBytes = baos.toByteArray();

            uploadToS3(key, imageBytes, file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload campus image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload campus banner
     */
    public String uploadCampusBanner(Long campusId, MultipartFile file) {
        try {
            // Security validation
            FileSecurityService.FileValidationResult validation = fileSecurityService.validateFile(file,
                    FileSecurityService.FileType.IMAGE);
            if (!validation.isValid()) {
                throw new RuntimeException("File security validation failed: " + validation.getErrorMessage());
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = "campus-" + campusId + "-banner-" + UUID.randomUUID().toString() + "." + fileExtension;
            String key = buildS3Key(CAMPUS_BANNERS_PREFIX + filename);

            // Optimize image for campus banner (larger size)
            BufferedImage optimizedImage = optimizeImage(file, 1200); // Large size for banners
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(optimizedImage, fileExtension, baos);
            byte[] imageBytes = baos.toByteArray();

            uploadToS3(key, imageBytes, file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload campus banner: " + e.getMessage(), e);
        }
    }

    /**
     * Upload document file
     */
    public String uploadDocument(String category, MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = category + "-" + UUID.randomUUID().toString() + "." + fileExtension;

            // Determine folder based on category
            String prefix = switch (category.toLowerCase()) {
                case "profile" -> PROFILE_DOCUMENTS_PREFIX;
                case "event" -> EVENT_DOCUMENTS_PREFIX;
                default -> TEMP_UPLOADS_PREFIX;
            };

            String key = buildS3Key(prefix + filename);

            uploadToS3(key, file.getBytes(), file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    /**
     * Generate presigned URL for secure file access
     */
    public String generatePresignedUrl(String key, Duration expiration) {
        try {
            // Determine content type based on file extension
            String contentType = getContentTypeFromKey(key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .responseContentType(contentType)
                    .responseCacheControl("public, max-age=3600")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    private String getContentTypeFromKey(String key) {
        String extension = key.substring(key.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "mp3":
                return "audio/mpeg";
            case "mp4":
                return "audio/mp4";
            case "wav":
                return "audio/wav";
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Upload file with custom key (for form submissions and other dynamic content)
     */
    public String uploadFile(MultipartFile file, String customKey) {
        try {
            String key = buildS3Key(customKey);
            uploadToS3Stream(key, file.getInputStream(), file.getSize(), file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Get public URL for a file
     */
    public String getPublicUrl(String key) {
        if (!s3Available || s3Client == null) {
            return "/uploads/" + key;
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    /**
     * Load file contents from S3 or local storage.
     *
     * @return the file bytes and content type, or null if the file does not exist
     */
    public StoredFile loadFile(String key) {
        try {
            if (key == null || key.isEmpty()) {
                return null;
            }
            if (s3Available && s3Client != null) {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                try (ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(getObjectRequest)) {
                    byte[] data = inputStream.readAllBytes();
                    String contentType = inputStream.response().contentType();
                    return new StoredFile(
                            data,
                            contentType != null ? contentType : getContentTypeFromKey(key));
                }
            }
            Path filePath = getLocalPath(key);
            if (!Files.exists(filePath)) {
                return null;
            }
            byte[] data = Files.readAllBytes(filePath);
            String contentType = getContentTypeFromKey(key);
            return new StoredFile(data, contentType);
        } catch (Exception e) {
            log.warn("Failed to load file '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Simple container for bytes served by loadFile.
     */
    public record StoredFile(byte[] data, String contentType) {}

    /**
     * Delete file from S3 using URL (extracts key from URL)
     */
    public void deleteFileByUrl(String fileUrl) {
        try {
            // Extract key from URL
            String key = extractKeyFromUrl(fileUrl);
            if (key != null && !key.isEmpty()) {
                deleteFile(key);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete file by URL: " + e.getMessage());
            // Don't throw exception for file deletion failures
        }
    }

    /**
     * Extract S3 key from full URL
     */
    private String extractKeyFromUrl(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                return null;
            }

            // Format: https://bucket-name.s3.region.amazonaws.com/key
            String pattern = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (fileUrl.startsWith(pattern)) {
                return fileUrl.substring(pattern.length());
            }

            // Alternative format: https://s3.region.amazonaws.com/bucket-name/key
            String altPattern = String.format("https://s3.%s.amazonaws.com/%s/", region, bucketName);
            if (fileUrl.startsWith(altPattern)) {
                return fileUrl.substring(altPattern.length());
            }

            return null;
        } catch (Exception e) {
            System.err.println("Failed to extract key from URL: " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String key) {
        if (!s3Available || s3Client == null) {
            deleteLocalFile(key);
            return;
        }
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

            // Also delete thumbnail if it's a profile image
            String relativePath = folderPrefix + PROFILE_IMAGES_PREFIX;
            if (key.startsWith(relativePath) && !key.contains("thumbnails/")) {
                String thumbnailKey = key.replace(relativePath, relativePath + "thumbnails/");
                try {
                    DeleteObjectRequest thumbDeleteRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(thumbnailKey)
                            .build();
                    s3Client.deleteObject(thumbDeleteRequest);
                } catch (Exception e) {
                    // Thumbnail might not exist, ignore error
                }
            }
        } catch (Exception e) {
            log.warn("S3 delete failed, falling back to local: {}", e.getMessage());
            s3Available = false;
            deleteLocalFile(key);
        }
    }

    private void deleteLocalFile(String key) {
        try {
            Path filePath = getLocalPath(key);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("Failed to delete local file: {}", e.getMessage());
        }
    }

    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String key) {
        if (!s3Available || s3Client == null) {
            return Files.exists(getLocalPath(key));
        }
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("S3 fileExists failed, checking locally: {}", e.getMessage());
            return Files.exists(getLocalPath(key));
        }
    }

    /**
     * Get file metadata
     */
    public Map<String, Object> getFileMetadata(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("contentType", response.contentType());
            metadata.put("contentLength", response.contentLength());
            metadata.put("lastModified", response.lastModified());
            metadata.put("etag", response.eTag());

            return metadata;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Copy file within S3
     */
    public String copyFile(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyRequest);
            return destinationKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy file: " + e.getMessage(), e);
        }
    }

    // Local storage fallback methods

    private Path getLocalPath(String key) {
        return Paths.get(localUploadDir, key);
    }

    private void saveToLocal(String key, byte[] data) {
        try {
            Path filePath = getLocalPath(key);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, data);
            log.info("Saved file locally: {}", filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save file locally: " + e.getMessage(), e);
        }
    }

    private void saveToLocal(String key, InputStream inputStream) {
        try {
            Path filePath = getLocalPath(key);
            Files.createDirectories(filePath.getParent());
            Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved file locally: {}", filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save file locally: " + e.getMessage(), e);
        }
    }

    // Private helper methods

    /**
     * Build full S3 key with environment prefix
     * e.g., "dev/courses/audio/file.mp3" or "prod/books/pdf/book.pdf"
     */
    private String buildS3Key(String relativePath) {
        // Ensure proper path separator between folderPrefix and relativePath
        if (folderPrefix.endsWith("/")) {
            return folderPrefix + relativePath;
        }
        return folderPrefix + "/" + relativePath;
    }

    private void uploadToS3(String key, byte[] data, String contentType) {
        if (!s3Available || s3Client == null) {
            saveToLocal(key, data);
            return;
        }
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("uploaded-by", "power-city-platform");
            metadata.put("upload-timestamp", String.valueOf(System.currentTimeMillis()));

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .metadata(metadata)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(data));
        } catch (Exception e) {
            log.warn("S3 upload failed, falling back to local storage: {}", e.getMessage());
            s3Available = false;
            saveToLocal(key, data);
        }
    }

    /**
     * Upload to S3 using streaming (for large files - avoids loading into memory)
     */
    private void uploadToS3Stream(String key, InputStream inputStream, long contentLength, String contentType) {
        if (!s3Available || s3Client == null) {
            saveToLocal(key, inputStream);
            return;
        }
        try {
            log.info("uploadToS3Stream - Starting upload to bucket: {}, key: {}, size: {}", bucketName, key, contentLength);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("uploaded-by", "power-city-platform");
            metadata.put("upload-timestamp", String.valueOf(System.currentTimeMillis()));

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .metadata(metadata)
                    .build();

            long start = System.currentTimeMillis();

            // Stream the file directly to S3 without loading into memory
            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));

            long duration = System.currentTimeMillis() - start;
            log.info("uploadToS3Stream - Upload completed in {}ms", duration);
        } catch (Exception e) {
            log.warn("S3 stream upload failed, falling back to local storage: {}", e.getMessage());
            s3Available = false;
            saveToLocal(key, inputStream);
        }
    }

    private BufferedImage optimizeImage(MultipartFile file, int targetSize) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());

        // Calculate new dimensions maintaining aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int newWidth, newHeight;
        if (originalWidth > originalHeight) {
            newWidth = targetSize;
            newHeight = (int) ((double) originalHeight * targetSize / originalWidth);
        } else {
            newHeight = targetSize;
            newWidth = (int) ((double) originalWidth * targetSize / originalHeight);
        }

        // Create optimized image
        BufferedImage optimizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = optimizedImage.createGraphics();

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return optimizedImage;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex + 1);
    }
}
