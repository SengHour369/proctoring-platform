package com.example.resultservice.report.entity;

import com.example.resultservice.common.entity.BaseEntity;
import com.example.resultservice.report.enums.ReportFormat;
import com.example.resultservice.report.enums.ReportStatus;
import com.example.resultservice.result.enums.IntegrityStatus;
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
import java.util.UUID;

/**
 * A rendered, immutable dossier for one attempt: candidate and exam details, the event timeline,
 * the evidence index, the risk score and the reviewer's decision, as they stood when it was
 * generated.
 *
 * <p>An entity and not just a query because the report is the artefact that leaves the system —
 * handed to an examinations board, attached to an appeal, disclosed under a data request. It must
 * therefore be versioned, checksummed, and reproducible after the live data has moved on or the
 * evidence behind it has been purged. {@code summarySnapshot} holds the frozen figures.
 */
@Entity
@Table(
        name = "proctoring_reports",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_proctoring_reports_public_id", columnNames = "public_id"),
                @UniqueConstraint(name = "uk_proctoring_reports_version", columnNames = {"exam_attempt_id", "version"})
        },
        indexes = {
                @Index(name = "ix_proctoring_reports_attempt", columnList = "exam_attempt_id"),
                @Index(name = "ix_proctoring_reports_status", columnList = "status")
        })
@Getter
@Setter
public class ProctoringReport extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "proctoring_session_id")
    private Long proctoringSessionId;

    /** The assessment version the report was rendered from. */
    @Column(name = "risk_assessment_id")
    private Long riskAssessmentId;

    @Column(name = "review_case_id")
    private Long reviewCaseId;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportStatus status = ReportStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportFormat format = ReportFormat.PDF;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "storage_path", length = 512)
    private String storagePath;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Integrity seal over the rendered file, so a copy in circulation can be proved genuine. */
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    /** Figures as rendered: event counts by severity, risk breakdown, score, timings. */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "summary_snapshot")
    private String summarySnapshot;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_integrity_status", length = 24)
    private IntegrityStatus finalIntegrityStatus;

    @Column(name = "includes_evidence", nullable = false)
    private boolean includesEvidence = false;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
