package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.sharedenums.ProctoringMode;
import com.example.proctoringservice.proctoring.enums.ProctoringSessionStatus;
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
 * The supervision envelope around one attempt: one session per attempt, created when the candidate
 * passes the pre-flight checks and closed when the attempt ends. It owns the live-monitoring state
 * (heartbeat, identity verification, assigned proctor); the observations themselves live in
 * {@link ProctoringEvent}.
 */
@Entity
@Table(
        name = "proctoring_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_proctoring_sessions_attempt", columnNames = "exam_attempt_id"),
                @UniqueConstraint(name = "uk_proctoring_sessions_public_id", columnNames = "public_id")
        },
        indexes = {
                @Index(name = "ix_proctoring_sessions_status", columnList = "status"),
                @Index(name = "ix_proctoring_sessions_proctor", columnList = "assigned_proctor_user_id")
        })
@Getter
@Setter
public class ProctoringSession extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProctoringSessionStatus status = ProctoringSessionStatus.PENDING;

    /** Copied from the exam policy at start; the policy may change afterwards, this may not. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProctoringMode mode = ProctoringMode.AI_ONLY;

    /** Set only in LIVE_PROCTOR / HYBRID mode. */
    @Column(name = "assigned_proctor_user_id")
    private Long assignedProctorUserId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "consent_accepted_at")
    private Instant consentAcceptedAt;

    @Column(name = "identity_verified", nullable = false)
    private boolean identityVerified = false;

    @Column(name = "identity_verified_at")
    private Instant identityVerifiedAt;

    @Column(name = "identity_verified_by_user_id")
    private Long identityVerifiedByUserId;

    @Column(name = "environment_scan_completed_at")
    private Instant environmentScanCompletedAt;

    /** Last agent ping. A stale value is itself an integrity signal, so it is stored, not derived. */
    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "heartbeat_miss_count", nullable = false)
    private int heartbeatMissCount = 0;

    /** Running counters, maintained on event ingest to keep monitoring dashboards off aggregates. */
    @Column(name = "event_count", nullable = false)
    private int eventCount = 0;

    @Column(name = "critical_event_count", nullable = false)
    private int criticalEventCount = 0;

    @Column(name = "warning_issued_count", nullable = false)
    private int warningIssuedCount = 0;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "termination_reason", length = 500)
    private String terminationReason;
}
