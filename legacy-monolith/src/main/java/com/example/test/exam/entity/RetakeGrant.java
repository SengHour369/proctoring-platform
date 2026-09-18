package com.example.test.exam.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A retake authorised for one candidate, separate from the {@code ReviewDecision} that judged it
 * was warranted. Conflating the two would make the entitlement disappear the moment the decision
 * row is superseded on appeal; keeping them apart means the grant survives, traceable back to
 * whichever decision (current or historical) authorised it.
 *
 * <p>Adds to the effective attempt cap for this one candidate only — it never mutates
 * {@code ExamAssignment.attemptsAllowed} or {@code Exam.maxAttempts}, so the exception stays
 * visible as an exception rather than blending into the exam's normal rules.
 */
@Entity
@Table(
        name = "retake_grants",
        indexes = {
                @Index(name = "ix_retake_grants_assignment", columnList = "exam_assignment_id"),
                @Index(name = "ix_retake_grants_candidate", columnList = "candidate_user_id")
        })
@Getter
@Setter
public class RetakeGrant extends BaseEntity {

    @Column(name = "exam_assignment_id", nullable = false)
    private Long examAssignmentId;

    @Column(name = "candidate_user_id", nullable = false)
    private Long candidateUserId;

    /** The GRANT_RETAKE decision this entitlement can't exist without. */
    @Column(name = "review_decision_id", nullable = false)
    private Long reviewDecisionId;

    @Column(name = "granted_by_user_id", nullable = false)
    private Long grantedByUserId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "additional_attempts", nullable = false)
    private int additionalAttempts = 1;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Set exactly once, atomically with the attempt it was consumed by. */
    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "consumed_by_attempt_id")
    private Long consumedByAttemptId;

    /** Required — a grant without a stated reason cannot be defended later. */
    @Column(nullable = false, length = 1000)
    private String reason;

    /** The grant this one revokes and replaces, when a retake is rescinded on further review. */
    @Column(name = "supersedes_grant_id")
    private Long supersedesGrantId;
}
