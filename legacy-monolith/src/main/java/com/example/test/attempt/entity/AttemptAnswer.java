package com.example.test.attempt.entity;

import com.example.test.attempt.enums.GradingStatus;
import com.example.test.common.entity.BaseEntity;
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
 * The candidate's response to one placed question. One row per (attempt, exam_question) — a
 * revision overwrites in place and bumps {@code revisionCount}, so the table stays one-row-per
 * question and remains cheap to score.
 *
 * <p>Auto-gradable types are scored on submit; ESSAY and SHORT_ANSWER wait at
 * {@link GradingStatus#PENDING} for a human.
 */
@Entity
@Table(
        name = "attempt_answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_attempt_answers", columnNames = {"exam_attempt_id", "exam_question_id"}))
@Getter
@Setter
public class AttemptAnswer extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "exam_question_id", nullable = false)
    private Long examQuestionId;

    /** Free-text response for SHORT_ANSWER, ESSAY, FILL_IN_BLANK and CODE. */
    @Column(name = "response_text", length = 8000)
    private String responseText;

    @Column(name = "response_numeric", precision = 18, scale = 6)
    private BigDecimal responseNumeric;

    @Column(name = "attachment_path", length = 512)
    private String attachmentPath;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "revision_count", nullable = false)
    private int revisionCount = 0;

    @Column(name = "time_spent_seconds", nullable = false)
    private int timeSpentSeconds = 0;

    /** Candidate-set "come back to this" marker, not an integrity flag. */
    @Column(name = "flagged_by_candidate", nullable = false)
    private boolean flaggedByCandidate = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", nullable = false, length = 24)
    private GradingStatus gradingStatus = GradingStatus.PENDING;

    /** Null until graded — distinct from FALSE, which means "graded and wrong". */
    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "points_awarded", precision = 9, scale = 2)
    private BigDecimal pointsAwarded;

    @Column(name = "graded_by_user_id")
    private Long gradedByUserId;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(name = "grader_comment", length = 2000)
    private String graderComment;
}
