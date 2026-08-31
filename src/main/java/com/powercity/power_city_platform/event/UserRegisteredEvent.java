package com.powercity.power_city_platform.event;

import com.powercity.power_city_platform.entity.User;

/**
 * Published when a user registers. The welcome/verification email is sent by a listener
 * after the registration transaction commits, so mail is only sent for persisted users
 * and SMTP never runs inside the DB transaction.
 */
public record UserRegisteredEvent(User user, String verificationToken) {}
