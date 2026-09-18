package com.example.test.excel.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One macro run inside an {@link ExcelSession} (25.4.8, 25.7.5). {@code allowed} records whether
 * the macro was on the exam's/question's whitelist at execution time — a run that wasn't is still
 * logged, not silently dropped, since a blocked-but-attempted run is itself the proctoring signal
 * behind {@code ProctoringEventType.EXCEL_MACRO_RUN}.
 */
@Entity
@Table(
        name = "excel_macro_executions",
        indexes = @Index(name = "ix_excel_macro_executions_session_time", columnList = "excel_session_id, executed_at"))
@Getter
@Setter
public class ExcelMacroExecution extends BaseEntity {

    @Column(name = "excel_session_id", nullable = false)
    private Long excelSessionId;

    @Column(name = "macro_name", nullable = false, length = 150)
    private String macroName;

    @Column(nullable = false)
    private boolean allowed = true;

    @Column(name = "input_summary", length = 1000)
    private String inputSummary;

    @Column(name = "output_summary", length = 1000)
    private String outputSummary;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();
}
