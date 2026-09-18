package com.example.test.attempt.entity;

import com.example.test.attempt.enums.TimingAnomalyType;
import com.example.test.common.entity.BaseEntity;
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
 * An answer that was correct, or high-scoring, in implausibly little time — a stronger signal
 * than a tab switch, and one that only exists once grading has happened, since a fast wrong
 * answer is not suspicious.
 *
 * <p>{@code expectedSeconds} is copied from {@link com.example.test.question.entity.Question} at
 * flag time so a later edit to the question's expected pacing can't reinterpret an old flag —
 * the same reasoning as a risk factor's weight being copied onto {@code RiskEvent}.
 */
@Entity
@Table(
        name = "answer_timing_anomalies",
        indexes = {
                @Index(name = "ix_answer_timing_anomalies_attempt", columnList = "exam_attempt_id"),
                @Index(name = "ix_answer_timing_anomalies_answer", columnList = "attempt_answer_id")
        })
@Getter
@Setter
public class AnswerTimingAnomaly extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "attempt_answer_id", nullable = false)
    private Long attemptAnswerId;

    @Column(name = "exam_question_id", nullable = false)
    private Long examQuestionId;

    @Column(name = "expected_seconds")
    private Integer expectedSeconds;

    @Column(name = "actual_seconds", nullable = false)
    private int actualSeconds;

    @Column(precision = 8, scale = 4)
    private BigDecimal ratio;

    /** Only fast *correct* or high-scoring answers are flagged — a fast wrong one is not a signal. */
    @Column(name = "answer_correct", nullable = false)
    private boolean answerCorrect;

    /** A single-revision correct answer in 4 seconds outranks one edited six times. */
    @Column(name = "revision_count", nullable = false)
    private int revisionCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false, length = 24)
    private TimingAnomalyType anomalyType;

    @Column(name = "flagged_at", nullable = false)
    private Instant flaggedAt = Instant.now();
}
