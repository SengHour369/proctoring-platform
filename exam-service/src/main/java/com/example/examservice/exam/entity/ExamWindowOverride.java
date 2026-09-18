package com.example.examservice.exam.entity;

import com.example.examservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A documented exception widening one candidate's sitting window, for cases an ordinary
 * assignment window can't cover — a verified medical absence, say. Scoped to the
 * {@code ExamAssignment}, never to the {@code Exam} itself: widening the exam's own
 * {@code opensAt}/{@code closesAt} to accommodate one candidate would change the window for
 * every other candidate too.
 */
@Entity
@Table(
        name = "exam_window_overrides",
        indexes = @Index(name = "ix_exam_window_overrides_assignment", columnList = "exam_assignment_id"))
@Getter
@Setter
public class ExamWindowOverride extends BaseEntity {

    @Column(name = "exam_assignment_id", nullable = false)
    private Long examAssignmentId;

    /** May fall outside the exam's own opensAt/closesAt — that's the point of an override. */
    @Column(name = "overridden_window_start_at", nullable = false)
    private Instant overriddenWindowStartAt;

    @Column(name = "overridden_window_end_at", nullable = false)
    private Instant overriddenWindowEndAt;

    @Column(nullable = false, length = 1000)
    private String reason;

    /** External ticket/document reference — an override without one is rejected at write time. */
    @Column(name = "justification_ref", length = 255)
    private String justificationRef;

    @Column(name = "granted_by_user_id", nullable = false)
    private Long grantedByUserId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    /** The override this one revokes and replaces, mirroring ReviewDecision.supersedesDecisionId. */
    @Column(name = "supersedes_override_id")
    private Long supersedesOverrideId;
}
