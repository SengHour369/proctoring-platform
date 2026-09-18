package com.example.identityservice.auth.entity;

import com.example.identityservice.auth.enums.ApiClientStatus;
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

/**
 * A non-human caller of the API: the AI inference workers that post detections, the evidence
 * uploader, an institution's student-information system pushing enrolments.
 *
 * <p>RBAC in {@code USERS}/{@code ROLES} covers people. These callers need the same treatment for
 * a different reason — the detection ingest endpoint accepts evidence about a candidate, so an
 * unauthenticated or over-scoped client is an integrity hole, not just a security one. Modelling
 * them as service accounts inside {@code USERS} was the alternative and is worse: it puts
 * credentials that never expire and never sign in through a login path built for humans.
 *
 * <p>Only the secret's hash is stored, and {@code allowedScopes} is deliberately narrow —
 * an inference worker should be able to write detections and read nothing.
 */
@Entity
@Table(
        name = "api_clients",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_api_clients_client_id", columnNames = "client_id"),
                @UniqueConstraint(name = "uk_api_clients_secret", columnNames = "client_secret_hash")
        },
        indexes = @Index(name = "ix_api_clients_status", columnList = "status"))
@Getter
@Setter
public class ApiClient extends BaseEntity {

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "client_secret_hash", nullable = false, length = 128)
    private String clientSecretHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApiClientStatus status = ApiClientStatus.ACTIVE;

    /** Space-separated permission codes, drawn from the same vocabulary as PERMISSIONS. */
    @Column(name = "allowed_scopes", nullable = false, length = 1000)
    private String allowedScopes;

    /** CIDR allow-list; an inference worker calls from a known network. */
    @Column(name = "allowed_ip_ranges", length = 500)
    private String allowedIpRanges;

    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "secret_rotated_at")
    private Instant secretRotatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "last_used_ip", length = 45)
    private String lastUsedIp;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;
}
