package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.entity.Course;
import com.powercity.power_city_platform.entity.Lesson;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.CourseProgressRepository;
import com.powercity.power_city_platform.repository.CourseRepository;
import com.powercity.power_city_platform.repository.LessonRepository;
import com.mpatric.mp3agic.Mp3File;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);

    private final LessonRepository lessonRepository;

    private final CourseRepository courseRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final CourseAudioStorageService courseAudioStorage;

    // Add lesson to a course
    public Lesson addLesson(Long courseId, String title, String description,
                           Long durationSeconds, Boolean isPreview,
                           MultipartFile audioFile, User currentUser) {
        // Get course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Validate audio file
        if (audioFile == null || audioFile.isEmpty()) {
            throw new RuntimeException("Audio file is required");
        }

        // Upload audio first; track the key so we can clean it up if the DB save fails.
        String audioKey = courseAudioStorage.uploadCourseAudio(courseId, audioFile);

        try {
            // Determine next order index
            Long lessonCount = lessonRepository.countByCourseId(courseId);
            int orderIndex = lessonCount.intValue();

            // Create lesson
            Lesson lesson = new Lesson();
            lesson.setCourse(course);
            lesson.setTitle(title);
            lesson.setDescription(description);
            lesson.setAudioKey(audioKey);
            // Prefer the server-read duration; fall back to whatever the browser detected.
            long detected = extractDurationSeconds(audioFile);
            lesson.setDurationSeconds(detected > 0 ? detected : (durationSeconds != null ? durationSeconds : 0L));
            lesson.setOrderIndex(orderIndex);
            lesson.setFileSizeBytes(audioFile.getSize());
            lesson.setIsPreview(isPreview != null ? isPreview : false);
            lesson.setCreatedBy(currentUser.getId());

            // Save lesson
            Lesson savedLesson = lessonRepository.save(lesson);

            // Update course totals
            course.updateLessonCount();
            course.updateTotalDuration();
            course.setUpdatedBy(currentUser.getId());
            courseRepository.save(course);

            log.info("Successfully added lesson {} to course {}", savedLesson.getId(), courseId);
            return savedLesson;

        } catch (Exception e) {
            // Don't leave an orphaned audio object behind if the lesson row couldn't be saved.
            try {
                courseAudioStorage.delete(audioKey);
            } catch (Exception cleanupError) {
                log.warn("Failed to clean up orphaned audio {} after a failed lesson save: {}", audioKey, cleanupError.getMessage());
            }
            log.error("Failed to add lesson to course {}: {}", courseId, e.getMessage(), e);
            throw new RuntimeException("Failed to add lesson. Please try again.");
        }
    }

    // Update lesson
    public Lesson updateLesson(Long lessonId, String title, String description,
                              Long durationSeconds, Boolean isPreview,
                              MultipartFile audioFile, User currentUser) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        if (title != null) lesson.setTitle(title);
        if (description != null) lesson.setDescription(description);
        if (durationSeconds != null) lesson.setDurationSeconds(durationSeconds);
        if (isPreview != null) lesson.setIsPreview(isPreview);

        // Upload new audio if provided
        if (audioFile != null && !audioFile.isEmpty()) {
            try {
                // Delete old audio file
                if (lesson.getAudioKey() != null) {
                    courseAudioStorage.delete(lesson.getAudioKey());
                }

                // Upload new audio
                String audioKey = courseAudioStorage.uploadCourseAudio(lesson.getCourse().getId(), audioFile);
                lesson.setAudioKey(audioKey);
                lesson.setFileSizeBytes(audioFile.getSize());

                // Re-read the duration from the new file.
                long detected = extractDurationSeconds(audioFile);
                if (detected > 0) lesson.setDurationSeconds(detected);

            } catch (Exception e) {
                log.error("Failed to upload audio file: {}", e.getMessage());
                throw new RuntimeException("Failed to upload audio file");
            }
        }

        lesson.setUpdatedBy(currentUser.getId());
        Lesson updatedLesson = lessonRepository.save(lesson);

        // Update course totals if duration changed
        Course course = lesson.getCourse();
        course.updateTotalDuration();
        course.setUpdatedBy(currentUser.getId());
        courseRepository.save(course);

        return updatedLesson;
    }

    // Delete lesson
    public void deleteLesson(Long lessonId, User currentUser) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        String audioKey = lesson.getAudioKey();
        Course course = lesson.getCourse();

        // Clear progress references first, or the FK on course_progress blocks the delete.
        courseProgressRepository.clearCurrentLessonReferences(lessonId);
        courseProgressRepository.removeLessonFromCompleted(lessonId);
        courseProgressRepository.flush();

        // Remove from the course; orphanRemoval deletes the row when the course is saved.
        // (Calling lessonRepository.delete() AND saving the course double-deletes and throws.)
        course.getLessons().remove(lesson);
        course.updateLessonCount();
        course.updateTotalDuration();
        course.setUpdatedBy(currentUser.getId());
        courseRepository.save(course);

        // Best-effort removal of the audio object once the row is gone.
        if (audioKey != null) {
            try {
                courseAudioStorage.delete(audioKey);
            } catch (Exception e) {
                log.error("Failed to delete audio {} from storage: {}", audioKey, e.getMessage());
            }
        }

        log.info("Successfully deleted lesson {}", lessonId);
    }

    /**
     * Read the clip length from the uploaded audio's MP3 metadata. Returns 0 if it can't be read
     * (non-MP3, corrupt header, etc.) — never throws, so it can't block an upload.
     */
    private long extractDurationSeconds(MultipartFile audioFile) {
        File temp = null;
        try {
            temp = File.createTempFile("lesson-audio-", ".mp3");
            try (InputStream in = audioFile.getInputStream()) {
                Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return new Mp3File(temp).getLengthInSeconds();
        } catch (Exception e) {
            log.warn("Could not read audio duration from upload: {}", e.getMessage());
            return 0L;
        } finally {
            if (temp != null) {
                //noinspection ResultOfMethodCallIgnored
                temp.delete();
            }
        }
    }

    // Reorder lessons
    public void reorderLessons(Long courseId, List<Long> lessonIds, User currentUser) {
        // Validate course exists
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Update order index for each lesson
        for (int i = 0; i < lessonIds.size(); i++) {
            Long lessonId = lessonIds.get(i);
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

            if (!lesson.getCourse().getId().equals(courseId)) {
                throw new RuntimeException("Lesson " + lessonId + " does not belong to course " + courseId);
            }

            lesson.setOrderIndex(i);
            lesson.setUpdatedBy(currentUser.getId());
            lessonRepository.save(lesson);
        }

        log.info("Successfully reordered {} lessons for course {}", lessonIds.size(), courseId);
    }

    // Get lesson by ID
    @Transactional(readOnly = true)
    public Lesson getLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
    }

    // Get all lessons for a course
    @Transactional(readOnly = true)
    public List<Lesson> getLessonsByCourse(Long courseId) {
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    // Get next lesson
    @Transactional(readOnly = true)
    public Optional<Lesson> getNextLesson(Long courseId, Integer currentIndex) {
        return lessonRepository.findNextLesson(courseId, currentIndex);
    }

    // Get previous lesson
    @Transactional(readOnly = true)
    public Optional<Lesson> getPreviousLesson(Long courseId, Integer currentIndex) {
        return lessonRepository.findPreviousLesson(courseId, currentIndex);
    }

    // Get first lesson
    @Transactional(readOnly = true)
    public Optional<Lesson> getFirstLesson(Long courseId) {
        return lessonRepository.findFirstLesson(courseId);
    }

    // Get lesson stream URL with purchase verification
    @Transactional(readOnly = true)
    public String getLessonStreamUrl(Long lessonId, User currentUser) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        // Verify user has purchased/enrolled in the course (unless it's a preview lesson)
        if (!lesson.getIsPreview()) {
            boolean hasPurchased = courseProgressRepository.hasUserPurchasedCourse(
                    currentUser.getId(),
                    lessonId
            );

            if (!hasPurchased) {
                throw new RuntimeException("You must purchase this course to access this lesson");
            }
        }

        // Generate presigned URL (1-hour expiry)
        String presignedUrl = courseAudioStorage.presignedUrl(lesson.getAudioKey(), Duration.ofHours(1));

        log.info("Generated stream URL for lesson {} for user {}", lessonId, currentUser.getId());
        return presignedUrl;
    }

    // Get preview lessons for a course
    @Transactional(readOnly = true)
    public List<Lesson> getPreviewLessons(Long courseId) {
        return lessonRepository.findByIsPreviewTrueAndCourseId(courseId);
    }
}
