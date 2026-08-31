package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.dto.response.course.CourseProgressDTO;
import com.powercity.power_city_platform.entity.CourseProgress;
import com.powercity.power_city_platform.entity.User;

import java.util.HashMap;
import java.util.Map;
import com.powercity.power_city_platform.service.CourseProgressService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/progress")
@Tag(name = "Course Progress", description = "APIs for tracking user progress in courses")
@RequiredArgsConstructor
public class CourseProgressController {

    private final CourseProgressService courseProgressService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(CourseProgressController.class);


    @GetMapping("/my-courses")
    @Operation(summary = "Get user's courses", description = "Get all courses the user is enrolled in")
    public ResponseEntity<ApiResponse<List<CourseProgressDTO>>> getUserCourses() {
        User currentUser = userService.getCurrentUser();
        List<CourseProgressDTO> courses = courseProgressService.getUserCoursesDTO(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
    }

    @GetMapping("/incomplete")
    @Transactional(readOnly = true)
    @Operation(summary = "Get incomplete courses", description = "Get courses the user has not completed")
    public ResponseEntity<ApiResponse<List<CourseProgress>>> getIncompleteCourses() {
        User currentUser = userService.getCurrentUser();
        List<CourseProgress> courses = courseProgressService.getIncompleteCourses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Incomplete courses retrieved successfully", courses));
    }

    @GetMapping("/completed")
    @Transactional(readOnly = true)
    @Operation(summary = "Get completed courses", description = "Get courses the user has completed")
    public ResponseEntity<ApiResponse<List<CourseProgress>>> getCompletedCourses() {
        User currentUser = userService.getCurrentUser();
        List<CourseProgress> courses = courseProgressService.getCompletedCourses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Completed courses retrieved successfully", courses));
    }

    @GetMapping("/recent")
    @Transactional(readOnly = true)
    @Operation(summary = "Get recently accessed courses", description = "Get courses the user recently accessed")
    public ResponseEntity<ApiResponse<List<CourseProgress>>> getRecentlyAccessedCourses() {
        User currentUser = userService.getCurrentUser();
        List<CourseProgress> courses = courseProgressService.getRecentlyAccessedCourses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Recently accessed courses retrieved successfully", courses));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get course progress", description = "Get progress for a specific course")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseProgress(
            @PathVariable Long courseId) {
        User currentUser = userService.getCurrentUser();
        Optional<Map<String, Object>> progressData = courseProgressService.getProgressData(
                currentUser.getId(), courseId);

        if (progressData.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No progress found", null));
        }

        return ResponseEntity.ok(ApiResponse.success("Progress retrieved successfully", progressData.get()));
    }

    @PostMapping("/update-position")
    @Operation(summary = "Update playback position", description = "Update current playback position (auto-save)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePlaybackPosition(
            @RequestParam Long courseId,
            @RequestParam Long lessonId,
            @RequestParam Long positionSeconds) {
        User currentUser = userService.getCurrentUser();
        CourseProgress progress = courseProgressService.updatePlaybackPosition(
                currentUser.getId(), courseId, lessonId, positionSeconds
        );

        // Return minimal data to avoid lazy loading issues
        Map<String, Object> response = new HashMap<>();
        response.put("positionSeconds", progress.getCurrentPositionSeconds());
        response.put("completionPercentage", progress.getCompletionPercentage());

        return ResponseEntity.ok(ApiResponse.success("Position updated", response));
    }

    @PostMapping("/mark-complete")
    @Operation(summary = "Mark lesson complete", description = "Mark a lesson as completed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markLessonComplete(
            @RequestParam Long courseId,
            @RequestParam Long lessonId) {
        User currentUser = userService.getCurrentUser();
        CourseProgress progress = courseProgressService.markLessonComplete(
                currentUser.getId(), courseId, lessonId
        );

        // Return minimal data to avoid lazy loading issues
        Map<String, Object> response = new HashMap<>();
        response.put("completionPercentage", progress.getCompletionPercentage());
        response.put("isCompleted", progress.getIsCompleted());

        return ResponseEntity.ok(ApiResponse.success("Lesson marked complete", response));
    }


    @GetMapping("/analytics/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get course progress analytics", description = "Get progress analytics for a course (Admin only)")
    public ResponseEntity<ApiResponse<CourseProgressService.CourseProgressAnalytics>> getCourseAnalytics(
            @PathVariable Long courseId) {
        CourseProgressService.CourseProgressAnalytics analytics = courseProgressService.getCourseAnalytics(courseId);
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }
}
