package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.dto.response.course.CourseWithEnrollmentDTO;
import com.powercity.power_city_platform.entity.Course;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.service.CourseService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = "Course Management", description = "APIs for managing online courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);


    @GetMapping
    @Operation(summary = "Get all courses", description = "Get all active courses with pagination and enrollment status")
    public ResponseEntity<ApiResponse<Page<CourseWithEnrollmentDTO>>> getAllCourses(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        // Get current user if authenticated, otherwise null
        Long userId = null;
        try {
            User currentUser = userService.getCurrentUser();
            userId = currentUser.getId();
        } catch (Exception ignored) {
            // User not authenticated, continue with null userId
        }

        Page<CourseWithEnrollmentDTO> courses = courseService.getAllCoursesWithEnrollmentStatus(
                userId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Get course by ID", description = "Get detailed information about a specific course")
    public ResponseEntity<ApiResponse<Course>> getCourseById(
            @Parameter(description = "Course ID", required = true) @PathVariable Long courseId) {
        Course course = courseService.getCourseByIdWithLessons(courseId);
        return ResponseEntity.ok(ApiResponse.success("Course retrieved successfully", course));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured courses", description = "Get list of featured courses")
    public ResponseEntity<ApiResponse<List<Course>>> getFeaturedCourses() {
        List<Course> courses = courseService.getFeaturedCourses();
        return ResponseEntity.ok(ApiResponse.success("Featured courses retrieved successfully", courses));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent courses", description = "Get recently added courses")
    public ResponseEntity<ApiResponse<List<Course>>> getRecentCourses(
            @RequestParam(defaultValue = "6") int limit) {
        List<Course> courses = courseService.getRecentCourses(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent courses retrieved successfully", courses));
    }

    @GetMapping("/top-enrolled")
    @Operation(summary = "Get top enrolled courses", description = "Get courses with most enrollments")
    public ResponseEntity<ApiResponse<List<Course>>> getTopEnrolledCourses(
            @RequestParam(defaultValue = "6") int limit) {
        List<Course> courses = courseService.getTopEnrolledCourses(limit);
        return ResponseEntity.ok(ApiResponse.success("Top enrolled courses retrieved successfully", courses));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get courses by category", description = "Get courses filtered by category")
    public ResponseEntity<ApiResponse<Page<Course>>> getCoursesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        Page<Course> courses = courseService.getCoursesByCategory(category, page, size);
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
    }

    @GetMapping("/search")
    @Operation(summary = "Search courses", description = "Search courses by title, description, or instructor")
    public ResponseEntity<ApiResponse<Page<Course>>> searchCourses(
            @RequestParam String searchTerm,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        Page<Course> courses;
        if (category != null) {
            courses = courseService.searchCoursesByCategory(category, searchTerm, page, size);
        } else {
            courses = courseService.searchCourses(searchTerm, page, size);
        }
        return ResponseEntity.ok(ApiResponse.success("Courses found", courses));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create course", description = "Create a new course (Admin only)")
    public ResponseEntity<ApiResponse<Course>> createCourse(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) String category,
            @RequestParam BigDecimal basePrice,
            @RequestParam(required = false) BigDecimal ngnPrice,
            @RequestParam(required = false, defaultValue = "USD") Currency baseCurrency,
            @RequestParam(required = false, defaultValue = "false") Boolean isFeatured,
            @RequestParam(required = false) MultipartFile thumbnail) {
        User currentUser = userService.getCurrentUser();
        Course course = courseService.createCourse(
                title, description, instructor, category,
                basePrice, ngnPrice, baseCurrency, isFeatured,
                thumbnail, currentUser
        );
        return ResponseEntity.ok(ApiResponse.success("Course created successfully", course));
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update course", description = "Update an existing course (Admin only)")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable Long courseId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal basePrice,
            @RequestParam(required = false) BigDecimal ngnPrice,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) MultipartFile thumbnail) {
        User currentUser = userService.getCurrentUser();
        Course course = courseService.updateCourse(
                courseId, title, description, instructor, category,
                basePrice, ngnPrice, isActive, isFeatured,
                thumbnail, currentUser
        );
        return ResponseEntity.ok(ApiResponse.success("Course updated successfully", course));
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete course", description = "Soft delete a course (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long courseId) {
        User currentUser = userService.getCurrentUser();
        courseService.deleteCourse(courseId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully", null));
    }

    @PostMapping("/{courseId}/enroll")
    @Operation(summary = "Enroll in a free course", description = "Directly enrol the current user in a free course (no payment). Paid courses must be purchased.")
    public ResponseEntity<ApiResponse<Void>> enrollInFreeCourse(@PathVariable Long courseId) {
        User currentUser = userService.getCurrentUser();
        courseService.enrollInFreeCourse(courseId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Enrolled successfully", null));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get course analytics", description = "Get course statistics (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseAnalytics() {
        Map<String, Object> analytics = courseService.getCourseAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }
}
