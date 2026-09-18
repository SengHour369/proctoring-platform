package com.example.identityservice.auth.notification;

/**
 * The identity module's one outbound email touchpoint. A real deployment would swap the default
 * implementation for one that calls an SMTP relay or {@code notification-service}; nothing else
 * in this module needs to change.
 */
public interface EmailSender {

    void sendMfaCode(String toEmail, String code);

    /** §2.1.1 step 6 — the plaintext invitation token, delivered as a link. */
    void sendAccountInvitation(String toEmail, String token);

    /** §2.1.2 step 5 — sent to the *new* address to confirm an email change. */
    void sendEmailChangeConfirmation(String toEmail, String token);

    /** §2.1.2 step 5 — optional notice to the *old* address; a mismatch here is worth alerting on. */
    void sendEmailChangeNotice(String oldEmail, String newEmail);
}
