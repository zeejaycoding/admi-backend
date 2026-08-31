package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.response.course.CourseWithEnrollmentDTO;
import com.powercity.power_city_platform.entity.Course;
import com.powercity.power_city_platform.entity.CourseProgress;
import com.powercity.power_city_platform.entity.Lesson;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.ProductType;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.CartItemRepository;
import com.powercity.power_city_platform.repository.CourseRepository;
import com.powercity.power_city_platform.repository.CourseProgressRepository;
import com.powercity.power_city_platform.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final CloudinaryService cloudinaryService;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseAudioStorageService courseAudioStorage;

    // Create a new course with thumbnail upload
    public Course createCourse(String title, String description, String instructor,
                               String category, BigDecimal basePrice, BigDecimal ngnPrice,
                               Currency baseCurrency, Boolean isFeatured,
                               MultipartFile thumbnailFile, User currentUser) {
        // Only an active course reserves a title; a soft-deleted one shouldn't block re-creation.
        if (courseRepository.existsByTitleAndIsActiveTrue(title)) {
            throw new RuntimeException("Course with this title already exists");
        }

        try {
            // Create the course
            Course course = new Course();
            course.setTitle(title);
            course.setDescription(description);
            course.setInstructor(instructor);
            course.setCategory(category);
            course.setBasePrice(basePrice);
            course.setNgnPrice(ngnPrice);
            course.setBaseCurrency(baseCurrency != null ? baseCurrency : Currency.USD);
            course.setIsFeatured(isFeatured != null ? isFeatured : false);
            course.setIsActive(true);
            course.setCreatedBy(currentUser.getId());
            course.setUpdatedBy(currentUser.getId());

            // Save course first to get the ID
            Course savedCourse = courseRepository.save(course);

            // Upload thumbnail to Cloudinary if provided
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                Map<String, String> thumbnailResult = cloudinaryService.uploadCourseThumbnail(
                    thumbnailFile,
                    savedCourse.getId()
                );
                savedCourse.setThumbnailUrl(thumbnailResult.get("url"));
                savedCourse.setThumbnailKey(thumbnailResult.get("publicId"));
            }

            // Save updated course with thumbnail
            return courseRepository.save(savedCourse);

        } catch (Exception e) {
            logger.error("Failed to create course. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create course. Please try again.");
        }
    }

    // Update an existing course
    public Course updateCourse(Long courseId, String title, String description, String instructor,
                              String category, BigDecimal basePrice, BigDecimal ngnPrice,
                              Boolean isActive, Boolean isFeatured,
                              MultipartFile thumbnailFile, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (title != null) course.setTitle(title);
        if (description != null) course.setDescription(description);
        if (instructor != null) course.setInstructor(instructor);
        if (category != null) course.setCategory(category);
        if (basePrice != null) course.setBasePrice(basePrice);
        if (ngnPrice != null) course.setNgnPrice(ngnPrice);
        if (isActive != null) course.setIsActive(isActive);
        if (isFeatured != null) course.setIsFeatured(isFeatured);

        // Upload new thumbnail if provided
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            try {
                // Delete old thumbnail if exists
                if (course.getThumbnailKey() != null) {
                    cloudinaryService.deleteFile(course.getThumbnailKey(), "image");
                }

                Map<String, String> thumbnailResult = cloudinaryService.uploadCourseThumbnail(
                    thumbnailFile,
                    courseId
                );
                course.setThumbnailUrl(thumbnailResult.get("url"));
                course.setThumbnailKey(thumbnailResult.get("publicId"));
            } catch (Exception e) {
                logger.error("Failed to upload thumbnail. Error: {}", e.getMessage());
            }
        }

        course.setUpdatedBy(currentUser.getId());
        course.setUpdatedAt(LocalDateTime.now());

        return courseRepository.save(course);
    }

    // Get course by ID
    @Transactional(readOnly = true)
    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    // Get course by ID with lessons loaded
    @Transactional(readOnly = true)
    public Course getCourseByIdWithLessons(Long courseId) {
        return courseRepository.findByIdWithLessons(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    // Get all active courses
    @Transactional(readOnly = true)
    public Page<Course> getAllCourses(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Course> courses = courseRepository.findByIsActiveTrue(pageable);
        // Initialize lessons collection to prevent lazy loading exception
        courses.forEach(course -> course.getLessons().size());
        return courses;
    }

    // Get all courses with enrollment status for a specific user
    @Transactional(readOnly = true)
    public Page<CourseWithEnrollmentDTO> getAllCoursesWithEnrollmentStatus(
            Long userId, int page, int size, String sortBy, String sortDirection) {
        Page<Course> courses = getAllCourses(page, size, sortBy, sortDirection);

        List<CourseWithEnrollmentDTO> courseDTOs = courses.getContent().stream()
                .map(course -> {
                    boolean isEnrolled = userId != null &&
                            courseProgressRepository.existsByUserIdAndCourseId(userId, course.getId());
                    return new CourseWithEnrollmentDTO(course, isEnrolled);
                })
                .collect(java.util.stream.Collectors.toList());

        return new PageImpl<>(courseDTOs, courses.getPageable(), courses.getTotalElements());
    }

    // Get courses by category
    @Transactional(readOnly = true)
    public Page<Course> getCoursesByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Course> courses = courseRepository.findByCategoryAndIsActiveTrue(category, pageable);
        courses.forEach(course -> course.getLessons().size());
        return courses;
    }

    // Search courses
    @Transactional(readOnly = true)
    public Page<Course> searchCourses(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Course> courses = courseRepository.searchCourses(searchTerm, pageable);
        courses.forEach(course -> course.getLessons().size());
        return courses;
    }

    // Search courses by category
    @Transactional(readOnly = true)
    public Page<Course> searchCoursesByCategory(String category, String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Course> courses = courseRepository.searchCoursesByCategory(category, searchTerm, pageable);
        courses.forEach(course -> course.getLessons().size());
        return courses;
    }

    // Get featured courses
    @Transactional(readOnly = true)
    public List<Course> getFeaturedCourses() {
        List<Course> courses = courseRepository.findByIsFeaturedTrueAndIsActiveTrue();
        courses.forEach(course -> course.getLessons().size());
        return courses;
    }

    // Get recent courses
    @Transactional(readOnly = true)
    public List<Course> getRecentCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Course> courses = courseRepository.findRecentCourses(pageable);
        courses.forEach(course -> course.getLessons().size());
        return courses;
    }

    // Get top enrolled courses
    @Transactional(readOnly = true)
    public List<Course> getTopEnrolledCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Course> courses = courseRepository.findTopEnrolledCourses(pageable);
        courses.forEach(course -> course.getLessons().size());
        return courses;
    }

    // Get longest courses
    @Transactional(readOnly = true)
    public List<Course> getLongestCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Course> courses = courseRepository.findLongestCourses(pageable);
        courses.forEach(course -> course.getLessons().size());
        return courses;
    }

    // Delete course (soft delete)
    public void deleteCourse(Long courseId, User currentUser) {
        // Load with lessons so we can clean up their audio files.
        Course course = courseRepository.findByIdWithLessons(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // 1. Drop the course link from any order lines (orders keep their price/quantity record).
        orderItemRepository.detachCourse(courseId);

        // 2. Remove the course from every cart.
        cartItemRepository.deleteByProductIdAndProductType(courseId, ProductType.COURSE);

        // 3. Remove learner progress: completed-lesson join rows first, then the progress rows.
        courseProgressRepository.deleteCompletedLessonJoinsByCourseId(courseId);
        courseProgressRepository.deleteByCourseId(courseId);
        courseProgressRepository.flush();

        // 4. Delete the audio objects for each lesson (best-effort).
        for (Lesson lesson : course.getLessons()) {
            if (lesson.getAudioKey() != null) {
                try {
                    courseAudioStorage.delete(lesson.getAudioKey());
                } catch (Exception e) {
                    logger.warn("Failed to delete audio {} during course delete: {}", lesson.getAudioKey(), e.getMessage());
                }
            }
        }

        // 5. Delete the course thumbnail (best-effort).
        if (course.getThumbnailKey() != null) {
            try {
                cloudinaryService.deleteFile(course.getThumbnailKey(), "image");
            } catch (Exception e) {
                logger.warn("Failed to delete thumbnail during course delete: {}", e.getMessage());
            }
        }

        // 6. Hard delete the course; cascade + orphanRemoval delete its lessons.
        courseRepository.delete(course);
        logger.info("Permanently deleted course {}", courseId);
    }

    // Increment enrollment count
    public void incrementEnrollmentCount(Long courseId) {
        Course course = getCourseById(courseId);
        course.incrementEnrollmentCount();
        courseRepository.save(course);
    }

    // Get course analytics
    @Transactional(readOnly = true)
    public Map<String, Object> getCourseAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalCourses", courseRepository.countActiveCourses());
        analytics.put("featuredCourses", courseRepository.countFeaturedCourses());
        analytics.put("totalEnrollments", courseRepository.countTotalEnrollments());
        analytics.put("averageEnrollments", courseRepository.findAverageEnrollmentCount());
        analytics.put("minPrice", courseRepository.findMinPrice());
        analytics.put("maxPrice", courseRepository.findMaxPrice());
        analytics.put("averagePrice", courseRepository.findAveragePrice());

        // Count by category (grouped over free-text category values)
        Map<String, Long> categoryStats = new HashMap<>();
        for (Object[] row : courseRepository.countCoursesGroupedByCategory()) {
            String category = (String) row[0];
            Long count = (Long) row[1];
            categoryStats.put(category, count);
        }
        analytics.put("coursesByCategory", categoryStats);

        return analytics;
    }

    // Check if user has enrolled in course
    @Transactional(readOnly = true)
    public boolean hasUserEnrolled(Long userId, Long courseId) {
        return courseProgressRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    // Enroll the current user in a FREE course directly (no cart/payment). Idempotent.
    public void enrollInFreeCourse(Long courseId, User user) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (!course.isFree()) {
            throw new IllegalStateException("This course is not free and must be purchased");
        }
        if (courseProgressRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            return;
        }
        courseProgressRepository.save(new CourseProgress(user, course));
        logger.info("User {} enrolled in free course {}", user.getId(), courseId);
    }
}
