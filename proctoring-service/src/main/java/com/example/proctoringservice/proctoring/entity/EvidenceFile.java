package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.proctoring.enums.EvidenceKind;
import com.example.proctoringservice.proctoring.enums.EvidenceUploadStatus;
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
 * Metadata for a captured artefact — webcam frame, screen clip, audio, ID scan. Only the pointer
 * and the checksum live in the database; bytes go to object storage.
 *
 * <p>Attached to the session and, where one exists, to the specific event that triggered the
 * capture. The event link is optional because periodic snapshots are taken with no event at all.
 * {@code retentionUntil} exists so a purge job can honour data-protection limits without having to
 * re-derive the policy that applied at capture time.
 */
@Entity
@Table(
        name = "evidence_files",
        uniqueConstraints = @UniqueConstraint(name = "uk_evidence_files_public_id", columnNames = "public_id"),
        indexes = {
                @Index(name = "ix_evidence_files_session", columnList = "proctoring_session_id, captured_at"),
                @Index(name = "ix_evidence_files_event", columnList = "proctoring_event_id"),
                @Index(name = "ix_evidence_files_retention", columnList = "retention_until")
        })
@Getter
@Setter
public class EvidenceFile extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "proctoring_session_id", nullable = false)
    private Long proctoringSessionId;

    @Column(name = "proctoring_event_id")
    private Long proctoringEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvidenceKind kind;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Tamper check — evidence that cannot be shown to be unaltered is not evidence. */
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "offset_ms")
    private Long offsetMs;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 24)
    private EvidenceUploadStatus uploadStatus = EvidenceUploadStatus.PENDING;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "retention_until")
    private Instant retentionUntil;

    @Column(name = "purged_at")
    private Instant purgedAt;
}
