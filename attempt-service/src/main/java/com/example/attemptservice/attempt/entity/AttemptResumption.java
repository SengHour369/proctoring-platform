package com.example.attemptservice.attempt.entity;

import com.example.attemptservice.attempt.enums.ResumptionReason;
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
 * A candidate reconnecting to a live attempt after their client dropped — a crash, a network
 * drop, a reboot. A resume is a session-token rotation, not a second concurrent session: the
 * previous token is invalidated in the same operation that issues the new one.
 *
 * <p>Distinct from {@link com.example.attemptservice.attempt.enums.PauseRequestStatus}-driven pausing:
 * that is a deliberate, approved stop; this is the client simply coming back. The gap itself is
 * still a signal — a long enough one is worth a {@code RiskEvent} — which is why it's recorded
 * here rather than silently reissuing a token.
 */
@Entity
@Table(
        name = "attempt_resumptions",
        indexes = @Index(name = "ix_attempt_resumptions_attempt", columnList = "exam_attempt_id"))
@Getter
@Setter
public class AttemptResumption extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ResumptionReason reason;

    /** Hash of the token being invalidated — never the token itself. */
    @Column(name = "previous_session_token_hash", length = 128)
    private String previousSessionTokenHash;

    @Column(name = "new_session_token_hash", length = 128)
    private String newSessionTokenHash;

    @Column(name = "resumed_at", nullable = false)
    private Instant resumedAt = Instant.now();

    /** From the attempt's last heartbeat to this resume — added to ExamAttempt.pausedSeconds. */
    @Column(name = "time_away_seconds", nullable = false)
    private long timeAwaySeconds;

    /** True when the gap was under the policy's grace window and needed no proctor decision. */
    @Column(name = "auto_approved", nullable = false)
    private boolean autoApproved = false;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    /** Set when the gap exceeded the grace window and was itself scored as a risk factor. */
    @Column(name = "risk_event_id")
    private Long riskEventId;
}
