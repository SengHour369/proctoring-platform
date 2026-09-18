package com.example.identityservice.auth.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link EmailSender}: logs instead of actually delivering. This repo has no SMTP/mail
 * infrastructure configured yet — replace this bean once one exists.
 */
@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void sendMfaCode(String toEmail, String code) {
        log.info("[stub email] MFA code for {}: {}", toEmail, code);
    }

    @Override
    public void sendAccountInvitation(String toEmail, String token) {
        log.info("[stub email] account invitation for {}: token={}", toEmail, token);
    }

    @Override
    public void sendEmailChangeConfirmation(String toEmail, String token) {
        log.info("[stub email] email-change confirmation for {}: token={}", toEmail, token);
    }

    @Override
    public void sendEmailChangeNotice(String oldEmail, String newEmail) {
        log.info("[stub email] notice to {}: this account's email is being changed to {}", oldEmail, newEmail);
    }
}
