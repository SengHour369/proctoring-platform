package com.example.test.review.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.review.enums.ReviewCaseStatus;
import com.example.test.review.enums.ReviewOutcome;
import com.example.test.review.enums.ReviewPriority;
import com.example.test.risk.entity.RiskAssessment;
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
 * A human investigation into one attempt, opened automatically when risk crosses the exam's review
 * threshold or manually by a proctor. The case is the queue item and the audit record; the
 * judgements inside it are {@link ReviewDecision} rows.
 *
 * <p>Opened against the attempt, with the triggering {@link RiskAssessment} recorded as evidence.
 * Keying it to the attempt rather than to the assessment matters because a recompute produces a new
 * assessment version and must not orphan an open case.
 *
 * <p>Deliberately not unique per attempt: an attempt cleared on integrity grounds can later be
 * reopened as a separate case on an appeal, and both histories must survive.
 */
@Entity
@Table(
        name = "review_cases",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_cases_number", columnNames = "case_number"),
        indexes = {
                @Index(name = "ix_review_cases_attempt", columnList = "exam_attempt_id"),
                @Index(name = "ix_review_cases_status", columnList = "status"),
                @Index(name = "ix_review_cases_reviewer", columnList = "assigned_reviewer_user_id"),
                @Index(name = "ix_review_cases_due", columnList = "due_at")
        })
@Getter
@Setter
public class ReviewCase extends BaseEntity {

    /** Human-quotable reference, e.g. RC-2026-000481. */
    @Column(name = "case_number", nullable = false, length = 32)
    private String caseNumber;

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "risk_assessment_id")
    private Long riskAssessmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReviewCaseStatus status = ReviewCaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReviewPriority priority = ReviewPriority.NORMAL;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    /** Null when the platform opened the case automatically. */
    @Column(name = "opened_by_user_id")
    private Long openedByUserId;

    @Column(name = "open_reason", nullable = false, length = 500)
    private String openReason;

    @Column(name = "assigned_reviewer_user_id")
    private Long assignedReviewerUserId;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached = false;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** Settled verdict, mirrored from the final {@link ReviewDecision} when the case closes. */
    @Enumerated(EnumType.STRING)
    @Column(name = "final_outcome", length = 32)
    private ReviewOutcome finalOutcome;

    @Column(name = "summary", length = 2000)
    private String summary;

    /** True once the candidate has been told the outcome. */
    @Column(name = "candidate_notified", nullable = false)
    private boolean candidateNotified = false;
}
