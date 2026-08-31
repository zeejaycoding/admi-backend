package com.powercity.power_city_platform.service.email;

import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WelcomeEmailService {

    @Autowired
    private EmailService emailService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendWelcomeEmail(User user, String verificationToken) {
        try {
            emailService.sendWelcomeEmail(user, verificationToken);
        } catch (Exception e) {
            // Log the error but don't let it affect the main transaction
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendEmailVerification(User user, String verificationToken) {
        try {
            emailService.sendEmailVerificationEmail(user, verificationToken);
        } catch (Exception e) {
            // Log the error but don't let it affect the main transaction
            System.err.println("Failed to send verification email: " + e.getMessage());
        }
    }
}
