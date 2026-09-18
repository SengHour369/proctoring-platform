package com.example.identityservice.auth.notification;

/**
 * The identity module's one outbound SMS touchpoint, used for {@code MfaMethod.SMS}. A real
 * deployment would swap the default implementation for one that calls an SMS gateway (Twilio,
 * SNS, etc.) or {@code notification-service}; nothing else in this module needs to change.
 */
public interface SmsSender {

    void sendMfaCode(String toPhoneNumber, String code);
}
