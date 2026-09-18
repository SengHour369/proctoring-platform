package com.example.attemptservice.excel.entity;

import com.example.attemptservice.common.entity.BaseEntity;
import com.example.attemptservice.excel.enums.ExcelIntegrityStatus;
import com.example.attemptservice.excel.enums.ExcelRuntimeEngine;
import com.example.attemptservice.excel.enums.ExcelSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One candidate's live Excel runtime session for an attempt — the sandboxed container instance
 * that holds their working copy of the workbook. One per {@code ExamAttempt}, the same 1:1 shape
 * as {@code ProctoringSession}; {@link ExcelCellEdit}, {@link ExcelSheetOperation} and
 * {@link ExcelMacroExecution} all hang off this row rather than the attempt directly, the same
 * reason {@code ProctoringEvent} hangs off {@code ProctoringSession} rather than the attempt.
 *
 * <p>Periodic and final workbook snapshots are not a separate table here — they reuse
 * {@code EvidenceFile} (kind {@code EXCEL_WORKBOOK_SNAPSHOT} / {@code EXCEL_FINAL_WORKBOOK}),
 * keyed by this session's {@code proctoringSessionId}, so binary storage and checksum handling
 * aren't duplicated.
 */
@Entity
@Table(
        name = "excel_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_excel_sessions_attempt", columnNames = "exam_attempt_id"),
                @UniqueConstraint(name = "uk_excel_sessions_public_id", columnNames = "public_id")
        },
        indexes = @Index(name = "ix_excel_sessions_proctoring_session", columnList = "proctoring_session_id"))
@Getter
@Setter
public class ExcelSession extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    /** Null when the exam's proctoring mode is NONE — evidence/event correlation is skipped, not required. */
    @Column(name = "proctoring_session_id")
    private Long proctoringSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_used", length = 24)
    private ExcelRuntimeEngine engineUsed;

    @Column(name = "sandbox_container_id", length = 128)
    private String sandboxContainerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExcelSessionStatus status = ExcelSessionStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    /** SHA-256 of the workbook exactly as submitted — compared against the runtime's own replay to set integrityStatus. */
    @Column(name = "final_checksum_sha256", length = 64)
    private String finalChecksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "integrity_status", length = 16)
    private ExcelIntegrityStatus integrityStatus;
}
