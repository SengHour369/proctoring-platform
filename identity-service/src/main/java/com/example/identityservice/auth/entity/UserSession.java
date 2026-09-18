package com.example.identityservice.auth.entity;

import com.example.identityservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Authenticated login session (the JWT refresh side). Distinct from PROCTORING_SESSIONS: this is
 * "who is signed in", not "who is being watched". Kept server-side so an administrator can revoke
 * a session, and so a mid-exam sign-in from a second device is detectable.
 */
@Entity
@Table(
        name = "user_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_sessions_token", columnNames = "refresh_token_hash"),
        indexes = {
                @Index(name = "ix_user_sessions_user", columnList = "user_id"),
                @Index(name = "ix_user_sessions_expires", columnList = "expires_at")
        })
@Getter
@Setter
public class UserSession extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Hash, never the token itself — a stolen table dump must not be replayable. */
    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "device_fingerprint", length = 128)
    private String deviceFingerprint;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 128)
    private String revokedReason;
}
