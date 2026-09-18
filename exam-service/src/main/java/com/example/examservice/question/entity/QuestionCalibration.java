package com.example.examservice.question.entity;

import com.example.examservice.common.entity.BaseEntity;
import com.example.examservice.question.enums.CalibrationAction;
import com.example.examservice.question.enums.CalibrationFlag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Classic-test-theory statistics for one question over one measurement window — difficulty
 * (proportion correct) and discrimination (correlation with total score) — computed only from
 * FINAL results, since a provisional one hasn't settled yet.
 *
 * <p>Negative discrimination (the strongest candidates get it wrong, the weakest get it right)
 * flags {@code MIS_KEY_SUSPECTED}: the most common real cause is a wrong answer key, not a bad
 * question. Either way this table only ever recommends — {@code recommendedAction = RETIRE} is
 * not itself a retirement; a human still calls {@code QuestionService.retireQuestion}.
 */
@Entity
@Table(
        name = "question_calibrations",
        indexes = @Index(name = "ix_question_calibrations_question", columnList = "question_id, window_start_at"))
@Getter
@Setter
public class QuestionCalibration extends BaseEntity {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "window_start_at", nullable = false)
    private Instant windowStartAt;

    @Column(name = "window_end_at", nullable = false)
    private Instant windowEndAt;

    @Column(name = "response_count", nullable = false)
    private long responseCount = 0;

    @Column(name = "correct_count", nullable = false)
    private long correctCount = 0;

    /** Proportion correct — 1.0 is a giveaway, 0.0 suggests a mis-keyed answer. */
    @Column(name = "difficulty_index", precision = 5, scale = 4)
    private BigDecimal difficultyIndex;

    /** Point-biserial correlation with total attempt score. */
    @Column(name = "discrimination_index", precision = 6, scale = 4)
    private BigDecimal discriminationIndex;

    @Column(name = "average_time_seconds", precision = 8, scale = 2)
    private BigDecimal averageTimeSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "flagged_reason", nullable = false, length = 24)
    private CalibrationFlag flaggedReason = CalibrationFlag.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, length = 16)
    private CalibrationAction recommendedAction = CalibrationAction.KEEP;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();
}
