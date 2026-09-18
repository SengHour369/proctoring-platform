package com.example.resultservice.result.entity;

import com.example.resultservice.common.entity.BaseEntity;
import com.example.resultservice.sharedenums.GradingMode;
import com.example.resultservice.result.enums.IntegrityStatus;
import com.example.resultservice.result.enums.ResultStatus;
import com.example.resultservice.sharedenums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The published outcome of an attempt — one row per attempt. Separate from ExamAttempt because a
 * result has its own lifecycle: it can be provisional, withheld pending an integrity review,
 * adjusted by a reviewer, released, or voided, all while the attempt itself stays unchanged.
 *
 * <p>{@code rawScore} is what the grader computed and {@code finalScore} what the candidate is
 * told; they diverge only through a recorded review decision, and keeping both makes any
 * adjustment visible rather than silent.
 *
 * <p>Exam and candidate are duplicated from the attempt on purpose: transcript and cohort
 * reporting query results directly, and this keeps those reads off a three-table join.
 */
@Entity
@Table(
        name = "exam_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_results_attempt", columnNames = "exam_attempt_id"),
        indexes = {
                @Index(name = "ix_exam_results_exam", columnList = "exam_id"),
                @Index(name = "ix_exam_results_candidate", columnList = "candidate_user_id"),
                @Index(name = "ix_exam_results_status", columnList = "status"),
                @Index(name = "ix_exam_results_integrity", columnList = "integrity_status")
        })
@Getter
@Setter
public class ExamResult extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "candidate_user_id", nullable = false)
    private Long candidateUserId;

    @Column(name = "raw_score", nullable = false, precision = 9, scale = 2)
    private BigDecimal rawScore = BigDecimal.ZERO;

    @Column(name = "score_adjustment", precision = 9, scale = 2)
    private BigDecimal scoreAdjustment;

    @Column(name = "final_score", nullable = false, precision = 9, scale = 2)
    private BigDecimal finalScore = BigDecimal.ZERO;

    @Column(name = "max_score", nullable = false, precision = 9, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "grade", length = 8)
    private String grade;

    /** Null while the result is provisional — pass/fail is only meaningful once final. */
    @Column(name = "passed")
    private Boolean passed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ResultStatus status = ResultStatus.PROVISIONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "integrity_status", nullable = false, length = 24)
    private IntegrityStatus integrityStatus = IntegrityStatus.CLEAN;

    /**
     * Risk score as it stood when the result was finalised, copied from the assessment. Frozen
     * rather than joined: a later recompute must not change a published result's stated basis.
     */
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 16)
    private RiskLevel riskLevel;

    /** The review that gated or adjusted this result, when there was one. */
    @Column(name = "review_case_id")
    private Long reviewCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_mode", nullable = false, length = 16)
    private GradingMode gradingMode = GradingMode.AUTO;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(name = "graded_by_user_id")
    private Long gradedByUserId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "released_to_candidate", nullable = false)
    private boolean releasedToCandidate = false;

    @Column(name = "correct_count", nullable = false)
    private int correctCount = 0;

    @Column(name = "incorrect_count", nullable = false)
    private int incorrectCount = 0;

    @Column(name = "unanswered_count", nullable = false)
    private int unansweredCount = 0;

    @Column(name = "pending_manual_count", nullable = false)
    private int pendingManualCount = 0;

    @Column(name = "time_spent_seconds")
    private Long timeSpentSeconds;

    @Column(name = "certificate_serial", length = 64)
    private String certificateSerial;
}
