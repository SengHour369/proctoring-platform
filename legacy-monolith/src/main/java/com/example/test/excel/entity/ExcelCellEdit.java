package com.example.test.excel.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Append-only history of one cell edit during an {@link ExcelSession} — the same role
 * {@code AnswerRevision} plays for a regular attempt answer. This is what recovers a workbook
 * after a crash mid-session (25.4.12), what a diff viewer (25.7.7) compares against the template,
 * and what a replay (25.7.8) steps through in {@code sequenceNo} order.
 */
@Entity
@Table(
        name = "excel_cell_edits",
        uniqueConstraints = @UniqueConstraint(name = "uk_excel_cell_edits_order", columnNames = {"excel_session_id", "sequence_no"}),
        indexes = @Index(name = "ix_excel_cell_edits_session_time", columnList = "excel_session_id, edited_at"))
@Getter
@Setter
public class ExcelCellEdit extends BaseEntity {

    @Column(name = "excel_session_id", nullable = false)
    private Long excelSessionId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "sheet_name", nullable = false, length = 100)
    private String sheetName;

    @Column(name = "cell_ref", nullable = false, length = 20)
    private String cellRef;

    @Column(name = "old_value", length = 2000)
    private String oldValue;

    @Column(name = "new_value", length = 2000)
    private String newValue;

    @Column(name = "old_formula", length = 1000)
    private String oldFormula;

    @Column(name = "new_formula", length = 1000)
    private String newFormula;

    @Column(name = "edited_at", nullable = false)
    private Instant editedAt = Instant.now();
}
