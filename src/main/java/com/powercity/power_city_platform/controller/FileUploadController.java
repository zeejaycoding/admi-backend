package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.service.S3FileService;
import com.powercity.power_city_platform.service.CloudinaryService;
import com.powercity.power_city_platform.service.UserService;
import com.powercity.power_city_platform.validation.ValidImageFile;
import com.powercity.power_city_platform.validation.ValidAudioFile;
import com.powercity.power_city_platform.validation.ValidPdfFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Management", description = "File upload and management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class FileUploadController {

    private final S3FileService s3FileService;
    private final CloudinaryService cloudinaryService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);


    @PostMapping("/profile-image")
    @Operation(summary = "Upload profile image", description = "Upload a profile image for the authenticated user")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfileImage(
            @Parameter(description = "Profile image file", required = true) @RequestParam("file") @ValidImageFile MultipartFile file) {
        // Get current user ID
        Long userId = userService.getCurrentUser().getId();

        // Upload image to S3
        String s3Key = s3FileService.uploadProfileImage(userId, file);

        // Update user profile with image URL and key
        String imageUrl = s3FileService.getPublicUrl(s3Key);
        userService.updateProfileImage(userId, imageUrl, s3Key);

        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", imageUrl);
        response.put("s3Key", s3Key);
        response.put("message", "Profile image uploaded successfully");

        return ResponseEntity.ok(ApiResponse.success("Profile image uploaded successfully", response));
    }

    @DeleteMapping("/profile-image")
    @Operation(summary = "Delete profile image", description = "Delete the profile image of the authenticated user")
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage() {
        // Get current user
        Long userId = userService.getCurrentUser().getId();

        // Delete from S3 and update user profile
        userService.deleteProfileImage(userId);

        return ResponseEntity.ok(ApiResponse.success("Profile image deleted successfully"));
    }

    @GetMapping("/profile-image/url")
    @Operation(summary = "Get profile image URL", description = "Get a presigned URL for the user's profile image")
    public ResponseEntity<ApiResponse<Map<String, String>>> getProfileImageUrl(
            @Parameter(description = "User ID (optional, defaults to current user)") @RequestParam(required = false) Long userId) {
        if (userId == null) {
            userId = userService.getCurrentUser().getId();
        }

        String presignedUrl = userService.getProfileImagePresignedUrl(userId);

        if (presignedUrl == null) {
            return ResponseEntity.ok(ApiResponse.success("No profile image found", null));
        }

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl);
        response.put("expiresIn", "1 hour");

        return ResponseEntity.ok(ApiResponse.success("Presigned URL generated successfully", response));
    }


    @PostMapping("/course-audio/{courseId}")
    @Operation(summary = "Upload course audio", description = "Upload audio files for a course (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCourseAudio(
            @Parameter(description = "Course ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "Audio file", required = true) @RequestParam("file") @ValidAudioFile MultipartFile file) {
        // Upload course audio to S3
        String s3Key = s3FileService.uploadCourseAudio(courseId, file);
        String fileUrl = s3FileService.getPublicUrl(s3Key);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", fileUrl);
        response.put("s3Key", s3Key);
        response.put("fileType", "audio");
        response.put("message", "Course audio uploaded successfully");

        return ResponseEntity.ok(ApiResponse.success("Course audio uploaded successfully", response));
    }

    @PostMapping("/course-material/{courseId}")
    @Operation(summary = "Upload course material", description = "Upload material files for a course (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCourseMaterial(
            @Parameter(description = "Course ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "Material file", required = true) @RequestParam("file") MultipartFile file) {
        // Upload course material to S3
        String s3Key = s3FileService.uploadCourseMaterial(courseId, file);
        String fileUrl = s3FileService.getPublicUrl(s3Key);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", fileUrl);
        response.put("s3Key", s3Key);
        response.put("fileType", "material");
        response.put("message", "Course material uploaded successfully");

        return ResponseEntity.ok(ApiResponse.success("Course material uploaded successfully", response));
    }

    @PostMapping("/course-content/{courseId}")
    @Operation(summary = "Upload course content", description = "Upload content files for a course (Admin only) - DEPRECATED: Use /course-audio or /course-material instead")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Deprecated
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCourseContent(
            @Parameter(description = "Course ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "Content file", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Content type") @RequestParam(defaultValue = "material") String contentType) {
        // Upload course content to S3
        String s3Key = s3FileService.uploadCourseContent(courseId, file, contentType);
        String fileUrl = s3FileService.getPublicUrl(s3Key);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", fileUrl);
        response.put("s3Key", s3Key);
        response.put("contentType", contentType);
        response.put("message", "Course content uploaded successfully");

        return ResponseEntity.ok(ApiResponse.success("Course content uploaded successfully", response));
    }


    @PostMapping("/book-pdf/{bookId}")
    @Operation(summary = "Upload book PDF", description = "Upload PDF file for a book (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBookPdf(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId,
            @Parameter(description = "PDF file", required = true) @RequestParam("file") @ValidPdfFile MultipartFile file) {
        // Upload book PDF to S3
        String s3Key = s3FileService.uploadBookPdf(bookId, file);
        String fileUrl = s3FileService.getPublicUrl(s3Key);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", fileUrl);
        response.put("s3Key", s3Key);
        response.put("fileType", "pdf");
        response.put("message", "Book PDF uploaded successfully");

        return ResponseEntity.ok(ApiResponse.success("Book PDF uploaded successfully", response));
    }

    @PostMapping("/book-cover/{bookId}")
    @Operation(summary = "Upload book cover", description = "Upload cover image for a book (Admin only) - Uses Cloudinary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBookCover(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId,
            @Parameter(description = "Cover image file", required = true) @RequestParam("file") @ValidImageFile MultipartFile file) throws IOException {
        // Upload book cover to Cloudinary (auto-optimized, WebP/AVIF conversion)
        Map<String, String> uploadResult = cloudinaryService.uploadBookCover(file, bookId);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", uploadResult.get("url"));
        response.put("publicId", uploadResult.get("publicId"));
        response.put("fileType", "cover-image");
        response.put("storage", "cloudinary");
        response.put("message", "Book cover uploaded successfully to Cloudinary");

        return ResponseEntity.ok(ApiResponse.success("Book cover uploaded successfully", response));
    }

    @PostMapping("/book-back-cover/{bookId}")
    @Operation(summary = "Upload book back cover", description = "Upload back cover image for a book (Admin only) - Uses Cloudinary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBookBackCover(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId,
            @Parameter(description = "Back cover image file", required = true) @RequestParam("file") @ValidImageFile MultipartFile file) throws IOException {
        // Upload book back cover to Cloudinary (auto-optimized, WebP/AVIF conversion)
        Map<String, String> uploadResult = cloudinaryService.uploadBookBackCover(file, bookId);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", uploadResult.get("url"));
        response.put("publicId", uploadResult.get("publicId"));
        response.put("fileType", "back-cover-image");
        response.put("storage", "cloudinary");
        response.put("message", "Book back cover uploaded successfully to Cloudinary");

        return ResponseEntity.ok(ApiResponse.success("Book back cover uploaded successfully", response));
    }


    @PostMapping("/event-image/{eventId}")
    @Operation(summary = "Upload event image", description = "Upload image for an event (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadEventImage(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId,
            @Parameter(description = "Event image file", required = true) @RequestParam("file") @ValidImageFile MultipartFile file) {
        // Upload event image to S3
        String s3Key = s3FileService.uploadEventImage(eventId, file);
        String fileUrl = s3FileService.getPublicUrl(s3Key);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", fileUrl);
        response.put("s3Key", s3Key);
        response.put("fileType", "event-image");
        response.put("message", "Event image uploaded successfully");

        return ResponseEntity.ok(ApiResponse.success("Event image uploaded successfully", response));
    }


    @PostMapping("/documents")
    @Operation(summary = "Upload document", description = "Upload a document file")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDocument(
            @Parameter(description = "Document file", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Document category") @RequestParam(defaultValue = "general") String category) {
        // Upload document to S3
        String s3Key = s3FileService.uploadDocument(category, file);
        String fileUrl = s3FileService.getPublicUrl(s3Key);

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", fileUrl);
        response.put("s3Key", s3Key);
        response.put("category", category);
        response.put("message", "Document uploaded successfully");

        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", response));
    }


    @GetMapping("/presigned-url")
    @Operation(summary = "Generate presigned URL", description = "Generate a presigned URL for secure file access")
    public ResponseEntity<ApiResponse<Map<String, String>>> generatePresignedUrl(
            @Parameter(description = "S3 file key", required = true) @RequestParam String s3Key,
            @Parameter(description = "Expiration hours") @RequestParam(defaultValue = "1") int expirationHours) {
        Duration expiration = Duration.ofHours(expirationHours);
        String presignedUrl = s3FileService.generatePresignedUrl(s3Key, expiration);

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl);
        response.put("expiresIn", expirationHours + " hours");
        response.put("s3Key", s3Key);

        return ResponseEntity.ok(ApiResponse.success("Presigned URL generated successfully", response));
    }

    @GetMapping("/metadata")
    @Operation(summary = "Get file metadata", description = "Get metadata for a file in S3")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFileMetadata(
            @Parameter(description = "S3 file key", required = true) @RequestParam String s3Key) {
        Map<String, Object> metadata = s3FileService.getFileMetadata(s3Key);
        return ResponseEntity.ok(ApiResponse.success("File metadata retrieved successfully", metadata));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete file", description = "Delete a file from S3 (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @Parameter(description = "S3 file key", required = true) @RequestParam String s3Key) {
        s3FileService.deleteFile(s3Key);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully"));
    }

    @PostMapping("/copy")
    @Operation(summary = "Copy file", description = "Copy a file within S3 (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> copyFile(
            @Parameter(description = "Source S3 key", required = true) @RequestParam String sourceKey,
            @Parameter(description = "Destination S3 key", required = true) @RequestParam String destinationKey) {
        String newKey = s3FileService.copyFile(sourceKey, destinationKey);
        String newUrl = s3FileService.getPublicUrl(newKey);

        Map<String, String> response = new HashMap<>();
        response.put("sourceKey", sourceKey);
        response.put("destinationKey", newKey);
        response.put("newUrl", newUrl);

        return ResponseEntity.ok(ApiResponse.success("File copied successfully", response));
    }

    @GetMapping("/exists")
    @Operation(summary = "Check file existence", description = "Check if a file exists in S3")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkFileExists(
            @Parameter(description = "S3 file key", required = true) @RequestParam String s3Key) {
        boolean exists = s3FileService.fileExists(s3Key);

        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);

        return ResponseEntity.ok(ApiResponse.success("File existence checked", response));
    }
}
