package com.example.identityservice.auth.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link SmsSender}: logs instead of actually delivering. This repo has no SMS gateway
 * configured yet — replace this bean once one exists.
 */
@Component
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void sendMfaCode(String toPhoneNumber, String code) {
        log.info("[stub sms] MFA code for {}: {}", toPhoneNumber, code);
    }
}
