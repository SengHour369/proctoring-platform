package com.example.identityservice.auth.entity;

import com.example.identityservice.auth.enums.MfaMethod;
import com.example.identityservice.auth.enums.UserStatus;
import com.example.identityservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Every human on the platform: candidates, proctors, reviewers and administrators. What a user may
 * do is not a column here but the set of {@link UserRole} grants attached to them, so one account
 * can be both a candidate and a reviewer without duplication.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_public_id", columnNames = "public_id"),
                @UniqueConstraint(name = "uk_users_external_ref", columnNames = "external_ref")
        },
        indexes = @Index(name = "ix_users_status", columnList = "status"))
@Getter
@Setter
public class User extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** Student / employee number from the system of record, when the user was provisioned there. */
    @Column(name = "external_ref", length = 64)
    private String externalRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    /**
     * Reference photo captured at enrolment. Face detections compare against this, so a candidate
     * without one cannot sit an exam whose policy requires an identity check.
     */
    @Column(name = "enrolment_photo_path", length = 512)
    private String enrolmentPhotoPath;

    /** Reference voiceprint, when the exam policy enables second-voice detection. */
    @Column(name = "voiceprint_path", length = 512)
    private String voiceprintPath;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "time_zone", length = 64)
    private String timeZone;

    @Column(length = 8)
    private String locale;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    /** Null means MFA is off. SMS/EMAIL methods reuse phoneNumber/email — no separate address column. */
    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_method", length = 16)
    private MfaMethod mfaMethod;

    /** Encrypted TOTP seed. Unused for SMS/EMAIL, which have no secret of their own to store. */
    @Column(name = "mfa_secret_encrypted", length = 255)
    private String mfaSecretEncrypted;

    @Column(name = "mfa_enrolled_at")
    private Instant mfaEnrolledAt;
}
