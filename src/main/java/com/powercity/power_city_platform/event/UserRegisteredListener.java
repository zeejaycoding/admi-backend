package com.powercity.power_city_platform.event;

import com.powercity.power_city_platform.service.email.WelcomeEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final WelcomeEmailService welcomeEmailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        welcomeEmailService.sendWelcomeEmail(event.user(), event.verificationToken());
    }
}
