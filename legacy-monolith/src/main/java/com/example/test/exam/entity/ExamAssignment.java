package com.example.test.exam.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.exam.enums.AssignmentStatus;
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
 * Entitlement of one candidate to sit one exam, with an optional per-candidate window and
 * accommodations. Attempts are only creatable against an assignment when the exam is invite-only,
 * which is what keeps "who was allowed to sit this" auditable after the fact.
 */
@Entity
@Table(
        name = "exam_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_assignments", columnNames = {"exam_id", "candidate_user_id"}),
        indexes = {
                @Index(name = "ix_exam_assignments_candidate", columnList = "candidate_user_id"),
                @Index(name = "ix_exam_assignments_status", columnList = "status")
        })
@Getter
@Setter
public class ExamAssignment extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "candidate_user_id", nullable = false)
    private Long candidateUserId;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "notified_at")
    private Instant notifiedAt;

    /** Candidate-specific window, narrowing the exam's own opens/closes range. */
    @Column(name = "window_start_at")
    private Instant windowStartAt;

    @Column(name = "window_end_at")
    private Instant windowEndAt;

    @Column(name = "due_at")
    private Instant dueAt;

    /** Overrides {@code Exam.maxAttempts} for this candidate (e.g. an approved retake). */
    @Column(name = "attempts_allowed")
    private Integer attemptsAllowed;

    /** Accessibility accommodation: extra minutes granted on top of the exam duration. */
    @Column(name = "extra_time_minutes")
    private Integer extraTimeMinutes;

    /** Hashed one-time code the candidate must present to open the exam. */
    @Column(name = "access_code_hash", length = 128)
    private String accessCodeHash;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
}
