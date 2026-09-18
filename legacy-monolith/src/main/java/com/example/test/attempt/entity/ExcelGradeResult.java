package com.example.test.attempt.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.question.enums.ExcelAnswerKind;
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
 * Outcome of grading one SPREADSHEET {@link AttemptAnswer} against one of its question's
 * {@code ExcelCellBinding}s. Needed for the same reason {@link CodeExecutionResult} exists for
 * CODE: {@code AttemptAnswer} is a single-row summary, and once a question grades several cells
 * independently, "which ones were correct" has nowhere else to live.
 *
 * <p>{@code graderType} is carried here as well as on the binding it grades, because the two can
 * diverge: an automated CHART/PIVOT comparison that came back inconclusive falls back to
 * {@code MANUAL} review, and this row is what records that the fallback actually happened.
 */
@Entity
@Table(
        name = "excel_grade_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_excel_grade_results",
                columnNames = {"attempt_answer_id", "excel_cell_binding_id"}))
@Getter
@Setter
public class ExcelGradeResult extends BaseEntity {

    @Column(name = "attempt_answer_id", nullable = false)
    private Long attemptAnswerId;

    @Column(name = "excel_cell_binding_id", nullable = false)
    private Long excelCellBindingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grader_type", nullable = false, length = 24)
    private ExcelAnswerKind graderType;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "points_awarded", precision = 9, scale = 2)
    private BigDecimal pointsAwarded;

    /** What the candidate's cell actually evaluated to at grading time. */
    @Column(name = "graded_value", length = 500)
    private String gradedValue;

    @Column(name = "graded_formula", length = 1000)
    private String gradedFormula;

    @Column(name = "grader_note", length = 1000)
    private String graderNote;

    @Column(name = "graded_at")
    private Instant gradedAt;
}
