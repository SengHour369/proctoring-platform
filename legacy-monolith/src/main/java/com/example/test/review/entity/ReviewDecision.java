package com.example.test.review.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.review.enums.ReviewDecisionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One judgement recorded against a case. Append-only and ordered: a case can pass through
 * "request more info", "escalate" and "invalidate", and each step keeps its own author, timestamp
 * and rationale. Exactly one row per case may be {@code finalDecision}.
 *
 * <p>An overturned decision is never edited or deleted — the superseding row points back at it.
 * That chain is what makes an appeal auditable.
 */
@Entity
@Table(
        name = "review_decisions",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_decisions_order", columnNames = {"review_case_id", "sequence_no"}))
@Getter
@Setter
public class ReviewDecision extends BaseEntity {

    @Column(name = "review_case_id", nullable = false)
    private Long reviewCaseId;

    @Column(name = "reviewer_user_id", nullable = false)
    private Long reviewerUserId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 32)
    private ReviewDecisionType decisionType;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt = Instant.now();

    /** Required: a decision without stated reasoning cannot be defended on appeal. */
    @Column(nullable = false, length = 2000)
    private String rationale;

    /** Evidence the reviewer relied on, as a list of EVIDENCE_FILES public ids. */
    @Column(name = "evidence_refs", length = 1000)
    private String evidenceRefs;

    /** Signed adjustment applied to the result when decision type is ADJUST_SCORE. */
    @Column(name = "score_adjustment", precision = 9, scale = 2)
    private BigDecimal scoreAdjustment;

    @Column(name = "is_final", nullable = false)
    private boolean finalDecision = false;

    /** The decision this one overturns, when a case is reopened or escalated. */
    @Column(name = "supersedes_decision_id")
    private Long supersedesDecisionId;
}
