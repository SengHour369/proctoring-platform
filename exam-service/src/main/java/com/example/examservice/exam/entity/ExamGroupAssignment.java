package com.example.examservice.exam.entity;

import com.example.examservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Assignment of an exam to a whole group. It does not replace {@link ExamAssignment}: expanding
 * this row fans out one per-candidate assignment per member, and those remain the single source of
 * truth for eligibility.
 *
 * <p>Keeping the group-level intent as its own row is what lets the system answer "why does this
 * student have this exam?" and re-expand correctly when a student joins the class late.
 */
@Entity
@Table(
        name = "exam_group_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_group_assignments", columnNames = {"exam_id", "student_group_id"}),
        indexes = @Index(name = "ix_exam_group_assignments_group", columnList = "student_group_id"))
@Getter
@Setter
public class ExamGroupAssignment extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_group_id", nullable = false)
    private Long studentGroupId;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "window_start_at")
    private Instant windowStartAt;

    @Column(name = "window_end_at")
    private Instant windowEndAt;

    @Column(name = "due_at")
    private Instant dueAt;

    /** Keep fanning out to students who join the group after this assignment was made. */
    @Column(name = "auto_enroll_new_members", nullable = false)
    private boolean autoEnrollNewMembers = true;

    @Column(name = "expanded_at")
    private Instant expandedAt;

    @Column(name = "expanded_count", nullable = false)
    private int expandedCount = 0;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
