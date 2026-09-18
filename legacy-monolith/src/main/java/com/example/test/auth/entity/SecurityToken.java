package com.example.test.auth.entity;

import com.example.test.auth.enums.TokenPurpose;
import com.example.test.common.entity.BaseEntity;
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

/**
 * One-time token behind email verification, forgot/reset password and account invitations. A single
 * table discriminated by {@link TokenPurpose} rather than one table per flow: every such token has
 * the same lifecycle — issued, expires, used once, then dead — and the same security rules.
 *
 * <p>Only the hash is stored, and {@code usedAt} is what makes it single-use; deleting the row
 * instead would lose the evidence that a reset actually happened.
 */
@Entity
@Table(
        name = "security_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_security_tokens_hash", columnNames = "token_hash"),
        indexes = {
                @Index(name = "ix_security_tokens_user", columnList = "user_id, purpose"),
                @Index(name = "ix_security_tokens_expires", columnList = "expires_at")
        })
@Getter
@Setter
public class SecurityToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    /** Where the token was redeemed — a mismatch with the request origin is worth alerting on. */
    @Column(name = "requested_ip", length = 45)
    private String requestedIp;

    @Column(name = "redeemed_ip", length = 45)
    private String redeemedIp;
}
