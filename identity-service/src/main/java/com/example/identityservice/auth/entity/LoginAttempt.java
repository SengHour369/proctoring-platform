package com.example.identityservice.auth.entity;

import com.example.identityservice.auth.enums.LoginOutcome;
import com.example.identityservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Login history, successes and failures alike. Kept separate from USER_SESSIONS because a failed
 * attempt creates no session, and separate from AUDIT_LOGS because it is written on an
 * unauthenticated path and read by rate limiting on every login.
 *
 * <p>{@code email} is recorded as typed: an attempt against an address that does not exist is
 * exactly what a credential-stuffing run looks like, and there is no user row to point at.
 */
@Entity
@Table(
        name = "login_attempts",
        indexes = {
                @Index(name = "ix_login_attempts_email_time", columnList = "email, attempted_at"),
                @Index(name = "ix_login_attempts_ip_time", columnList = "ip_address, attempted_at"),
                @Index(name = "ix_login_attempts_user", columnList = "user_id")
        })
@Getter
@Setter
public class LoginAttempt extends BaseEntity {

    /** Null when the address matched no account. */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LoginOutcome outcome;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt = Instant.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "device_fingerprint", length = 128)
    private String deviceFingerprint;

    @Column(name = "geo_country", length = 2)
    private String geoCountry;

    @Column(name = "failure_detail", length = 255)
    private String failureDetail;
}
