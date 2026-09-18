package com.example.test.review.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.review.enums.ItemVerdict;
import com.example.test.review.enums.ReviewItemKind;
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
 * A reviewer's verdict on one flagged item inside a case — this event, this detection, this clip:
 * valid, false positive, or needs investigation.
 *
 * <p>The case-level decision says what happens to the candidate; these rows say which signals
 * earned it. Two things depend on that distinction: a case can be dismissed while three of its
 * eight flags were genuine, and per-item false-positive counts are the only trustworthy input to
 * model performance and risk reweighting.
 *
 * <p>The referenced item is addressed by kind plus id rather than four nullable foreign keys,
 * since the target may be an event, a detection, a correlated activity or an evidence file. The
 * cost of that choice is that referential integrity for {@code itemId} is enforced in code.
 */
@Entity
@Table(
        name = "review_findings",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_findings_item",
                columnNames = {"review_case_id", "item_kind", "item_id"}),
        indexes = {
                @Index(name = "ix_review_findings_case", columnList = "review_case_id"),
                @Index(name = "ix_review_findings_verdict", columnList = "verdict")
        })
@Getter
@Setter
public class ReviewFinding extends BaseEntity {

    @Column(name = "review_case_id", nullable = false)
    private Long reviewCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_kind", nullable = false, length = 32)
    private ReviewItemKind itemKind;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ItemVerdict verdict;

    @Column(name = "reviewer_user_id", nullable = false)
    private Long reviewerUserId;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt = Instant.now();

    @Column(length = 1000)
    private String comment;

    /** True when the reviewer disagrees with how the engine weighted this item. */
    @Column(name = "weight_disputed", nullable = false)
    private boolean weightDisputed = false;
}
