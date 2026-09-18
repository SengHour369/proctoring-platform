package com.example.examservice.exam.entity;

import com.example.examservice.common.entity.BaseEntity;
import com.example.examservice.exam.enums.ExamStatus;
import com.example.examservice.exam.enums.GradingMode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import java.util.UUID;

/**
 * The exam definition — a blueprint, not a sitting. Once PUBLISHED its structure is frozen:
 * attempts in flight reference sections and questions through EXAM_QUESTIONS, so edits after
 * publication would silently rewrite history. Structural edits create a new {@code version}.
 */
@Entity
@Table(
        name = "exams",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_exams_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_exams_public_id", columnNames = "public_id")
        },
        indexes = {
                @Index(name = "ix_exams_status", columnList = "status"),
                @Index(name = "ix_exams_window", columnList = "opens_at, closes_at")
        })
@Getter
@Setter
public class Exam extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 4000)
    private String instructions;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "version", nullable = false)
    private int version = 1;

    /** Wall-clock budget for one attempt. Null means the closing time is the only limit. */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "opens_at")
    private Instant opensAt;

    @Column(name = "closes_at")
    private Instant closesAt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 1;

    @Column(name = "total_points", precision = 9, scale = 2)
    private BigDecimal totalPoints;

    @Column(name = "passing_score", precision = 9, scale = 2)
    private BigDecimal passingScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_mode", nullable = false, length = 32)
    private GradingMode gradingMode = GradingMode.AUTO;

    @Column(name = "shuffle_sections", nullable = false)
    private boolean shuffleSections = false;

    /** Withhold scores until any integrity review closes, instead of releasing on submit. */
    @Column(name = "hold_results_for_review", nullable = false)
    private boolean holdResultsForReview = false;

    @Column(name = "show_result_immediately", nullable = false)
    private boolean showResultImmediately = false;

    @Embedded
    private ProctoringPolicy proctoringPolicy = new ProctoringPolicy();

    @Embedded
    private ExcelPolicy excelPolicy = new ExcelPolicy();
}
