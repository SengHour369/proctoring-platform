package com.example.test.question.entity;

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

/**
 * One graded cell or range within a SPREADSHEET question's workbook. Hangs off {@link Question},
 * not off a placement in an exam — same reasoning as {@link CodeTestCase}: a binding is a
 * property of the bank item, so reusing the question in a second exam must not clone it.
 *
 * <p>Cells the author only wants locked from editing (25.1.5 "protected cells") are not modelled
 * here — sheet/cell protection is a property of the workbook file itself, enforced by the runtime
 * reading the file's own protection flags, not a separate rubric concern.
 */
@Entity
@Table(
        name = "excel_cell_bindings",
        uniqueConstraints = @UniqueConstraint(name = "uk_excel_cell_bindings_order", columnNames = {"question_id", "sequence_no"}))
@Getter
@Setter
public class ExcelCellBinding extends BaseEntity {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "sheet_name", nullable = false, length = 100)
    private String sheetName;

    @Column(name = "cell_ref", length = 20)
    private String cellRef;

    /** Set instead of cellRef for a RANGE/chart-source/pivot-source binding. */
    @Column(name = "range_ref", length = 40)
    private String rangeRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_kind", nullable = false, length = 24)
    private ExcelAnswerKind answerKind;

    /** Shown to the candidate, e.g. "Q1: Total Revenue". */
    @Column(length = 150)
    private String label;

    @Column(name = "expected_value", length = 500)
    private String expectedValue;

    @Column(name = "expected_formula", length = 1000)
    private String expectedFormula;

    /** Tolerance for a numeric VALUE comparison. */
    @Column(precision = 12, scale = 6)
    private BigDecimal tolerance;

    /** Null falls back to equal weighting across all of the question's bindings. */
    @Column(precision = 9, scale = 2)
    private BigDecimal points;
}
