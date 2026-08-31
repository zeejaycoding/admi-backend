package com.powercity.power_city_platform.service.email;

import com.powercity.power_city_platform.dto.request.email.SendEmailRequest;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CourseEnrollmentEmailService {

    @Autowired
    private EmailService emailService;

    @Value("${app.email.frontend-url}")
    private String frontendUrl;

    @Async("emailTaskExecutor")
    public void sendCourseEnrollmentConfirmation(User user, String courseName, String courseId) {
        try {
            Thread.sleep(25000);  // Longer delay to avoid collision with book emails
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String courseLink = frontendUrl + "/courses/" + courseId;

        Map<String, Object> templateData = Map.of(
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "lastName", user.getLastName() != null ? user.getLastName() : "",
                "courseName", courseName != null ? courseName : "Course",
                "courseLink", courseLink,
                "region", user.getRegion() != null ? user.getRegion() : "Global"
        );

        SendEmailRequest request = new SendEmailRequest(
                List.of(user.getEmail()),
                "Course Enrollment Confirmation - " + courseName,
                "course-enrollment-confirmation",
                templateData,
                null,
                null
        );

        emailService.sendEmail(request);
    }
}