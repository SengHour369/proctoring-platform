package com.example.attemptservice.attempt.entity;

import com.example.attemptservice.attempt.enums.AttemptStatus;
import com.example.attemptservice.common.entity.BaseEntity;
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
 * One sitting of one exam by one candidate — the spine of the whole model. Everything that happens
 * during or after an exam (answers, supervision, risk, review, results) hangs off an attempt, not
 * off the exam or the candidate, because the attempt is the only thing that is unique per event.
 *
 * <p>Note this differs from the sketch, where EXAM_ATTEMPTS descends from QUESTIONS. An attempt is
 * a candidate sitting an exam; it has no dependency on any single question. The question-level
 * link lives one level down, in {@link AttemptAnswer}.
 */
@Entity
@Table(
        name = "exam_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_exam_attempts_public_id", columnNames = "public_id"),
                @UniqueConstraint(name = "uk_exam_attempts_no", columnNames = {"exam_id", "candidate_user_id", "attempt_no"})
        },
        indexes = {
                @Index(name = "ix_exam_attempts_candidate", columnList = "candidate_user_id"),
                @Index(name = "ix_exam_attempts_status", columnList = "status"),
                @Index(name = "ix_exam_attempts_started", columnList = "started_at")
        })
@Getter
@Setter
public class ExamAttempt extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "candidate_user_id", nullable = false)
    private Long candidateUserId;

    /** Null for open exams that need no invitation. */
    @Column(name = "exam_assignment_id")
    private Long examAssignmentId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AttemptStatus status = AttemptStatus.NOT_STARTED;

    @Column(name = "started_at")
    private Instant startedAt;

    /** Hard deadline computed at start from duration + accommodations; drives auto-submit. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    /**
     * Hash of the attempt-scoped session token. Bound to the attempt rather than to the login
     * session, so a second tab or a second device cannot drive the same sitting.
     */
    @Column(name = "session_token_hash", length = 128)
    private String sessionTokenHash;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "time_spent_seconds", nullable = false)
    private long timeSpentSeconds = 0L;

    /** Cumulative paused time, so time_spent stays honest across proctor-ordered pauses. */
    @Column(name = "paused_seconds", nullable = false)
    private long pausedSeconds = 0L;

    @Column(name = "current_section_id")
    private Long currentSectionId;

    /** Snapshot of the exam version delivered, so a later re-publish cannot rewrite this sitting. */
    @Column(name = "exam_version", nullable = false)
    private int examVersion = 1;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "invalidation_reason", length = 500)
    private String invalidationReason;
}
