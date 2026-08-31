package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.Lesson;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.LessonService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lessons")
@Tag(name = "Lesson Management", description = "APIs for managing course lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(LessonController.class);


    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get course lessons", description = "Get all lessons for a course")
    public ResponseEntity<ApiResponse<List<Lesson>>> getCourseLessons(
            @PathVariable Long courseId) {
        List<Lesson> lessons = lessonService.getLessonsByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success("Lessons retrieved successfully", lessons));
    }

    @GetMapping("/{lessonId}")
    @Operation(summary = "Get lesson by ID", description = "Get detailed information about a lesson")
    public ResponseEntity<ApiResponse<Lesson>> getLessonById(
            @PathVariable Long lessonId) {
        Lesson lesson = lessonService.getLessonById(lessonId);
        return ResponseEntity.ok(ApiResponse.success("Lesson retrieved successfully", lesson));
    }

    @GetMapping("/{lessonId}/stream")
    @Operation(summary = "Get lesson stream URL", description = "Get presigned URL to stream lesson audio (requires purchase)")
    public ResponseEntity<ApiResponse<Map<String, String>>> getLessonStreamUrl(
            @PathVariable Long lessonId) {
        User currentUser = userService.getCurrentUser();
        String streamUrl = lessonService.getLessonStreamUrl(lessonId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Stream URL generated", Map.of("streamUrl", streamUrl)));
    }

    @GetMapping("/course/{courseId}/preview")
    @Operation(summary = "Get preview lessons", description = "Get preview lessons for a course (free access)")
    public ResponseEntity<ApiResponse<List<Lesson>>> getPreviewLessons(
            @PathVariable Long courseId) {
        List<Lesson> lessons = lessonService.getPreviewLessons(courseId);
        return ResponseEntity.ok(ApiResponse.success("Preview lessons retrieved successfully", lessons));
    }


    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Add lesson", description = "Add a new lesson to a course (Admin only)")
    public ResponseEntity<ApiResponse<Lesson>> addLesson(
            @PathVariable Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "0") Long durationSeconds,
            @RequestParam(required = false, defaultValue = "false") Boolean isPreview,
            @RequestParam MultipartFile audioFile) {
        User currentUser = userService.getCurrentUser();
        Lesson lesson = lessonService.addLesson(
                courseId, title, description, durationSeconds,
                isPreview, audioFile, currentUser
        );
        return ResponseEntity.ok(ApiResponse.success("Lesson added successfully", lesson));
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update lesson", description = "Update a lesson (Admin only)")
    public ResponseEntity<ApiResponse<Lesson>> updateLesson(
            @PathVariable Long lessonId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long durationSeconds,
            @RequestParam(required = false) Boolean isPreview,
            @RequestParam(required = false) MultipartFile audioFile) {
        User currentUser = userService.getCurrentUser();
        Lesson lesson = lessonService.updateLesson(
                lessonId, title, description, durationSeconds,
                isPreview, audioFile, currentUser
        );
        return ResponseEntity.ok(ApiResponse.success("Lesson updated successfully", lesson));
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete lesson", description = "Delete a lesson (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(@PathVariable Long lessonId) {
        User currentUser = userService.getCurrentUser();
        lessonService.deleteLesson(lessonId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted successfully", null));
    }

    @PutMapping("/course/{courseId}/reorder")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reorder lessons", description = "Reorder lessons in a course (Admin only)")
    public ResponseEntity<ApiResponse<Void>> reorderLessons(
            @PathVariable Long courseId,
            @RequestBody List<Long> lessonIds) {
        User currentUser = userService.getCurrentUser();
        lessonService.reorderLessons(courseId, lessonIds, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lessons reordered successfully", null));
    }
}
