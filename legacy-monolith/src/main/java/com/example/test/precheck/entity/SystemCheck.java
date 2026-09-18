package com.example.test.precheck.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.precheck.enums.SystemCheckStatus;
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
 * One pre-flight run: the candidate's device tested against the exam's requirements before the
 * paper is released. A run exists before any attempt does — that is the point of it — so
 * {@code attempt} is filled in only if the candidate went on to start.
 *
 * <p>Kept as a record rather than a transient check because "my camera was working" is the first
 * thing disputed after a failed sitting, and because a run that failed and was retried five times
 * from three networks is itself a signal.
 */
@Entity
@Table(
        name = "system_checks",
        indexes = {
                @Index(name = "ix_system_checks_candidate", columnList = "candidate_user_id, started_at"),
                @Index(name = "ix_system_checks_exam", columnList = "exam_id"),
                @Index(name = "ix_system_checks_attempt", columnList = "exam_attempt_id")
        })
@Getter
@Setter
public class SystemCheck extends BaseEntity {

    @Column(name = "candidate_user_id", nullable = false)
    private Long candidateUserId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    /** Set once the candidate proceeds, linking the run to the sitting it cleared. */
    @Column(name = "exam_attempt_id")
    private Long examAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SystemCheckStatus status = SystemCheckStatus.IN_PROGRESS;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo = 1;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    /** How long the clearance is good for; a stale run must be re-run before starting. */
    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "device_fingerprint", length = 128)
    private String deviceFingerprint;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "download_mbps", precision = 8, scale = 2)
    private java.math.BigDecimal downloadMbps;

    @Column(name = "upload_mbps", precision = 8, scale = 2)
    private java.math.BigDecimal uploadMbps;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "failed_check_count", nullable = false)
    private int failedCheckCount = 0;

    /** True when a proctor let the candidate through despite a failed item. */
    @Column(name = "overridden", nullable = false)
    private boolean overridden = false;

    @Column(name = "overridden_by_user_id")
    private Long overriddenByUserId;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;
}
