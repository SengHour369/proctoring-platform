package com.example.attemptservice.attempt.entity;

import com.example.attemptservice.attempt.enums.PauseRequestStatus;
import com.example.attemptservice.common.entity.BaseEntity;
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
 * A request to pause a live attempt, and its approval. {@code ProctoringEventType}
 * already has ATTEMPT_PAUSED/ATTEMPT_RESUMED, but an event log has no PENDING state to gate a
 * live decision on — a proctor needs something to act on, not just a record that a pause
 * happened. Same mutable-state-with-audit-trail shape this model already uses for
 * IdentityVerification, SystemCheck and ReviewDecision.
 */
@Entity
@Table(
        name = "attempt_pause_requests",
        indexes = {
                @Index(name = "ix_attempt_pause_requests_attempt", columnList = "exam_attempt_id"),
                @Index(name = "ix_attempt_pause_requests_status", columnList = "status")
        })
@Getter
@Setter
public class AttemptPauseRequest extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    /** Null covers a system-initiated pause (e.g. a dropped connection), not candidate or proctor. */
    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PauseRequestStatus status = PauseRequestStatus.PENDING;

    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    /** Distinct from decidedAt — approval and the candidate actually resuming aren't simultaneous. */
    @Column(name = "resumed_at")
    private Instant resumedAt;
}
