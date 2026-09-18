package com.example.test.proctoring.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.proctoring.enums.CustodyTransition;
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

/**
 * One step in an evidence file's chain of custody. {@link EvidenceFile#getChecksumSha256()}
 * proves the bytes are unaltered at rest; this table proves they were unaltered at every step in
 * between — captured, hashed, uploaded, verified — which a storage checksum alone can't show.
 *
 * <p>Complements rather than duplicates {@link EvidenceAccessLog}: the access log is the *who
 * looked*, this is the *chain* — every access-log write also writes an {@code ACCESSED} row here.
 * The uploader is identified by an {@link com.example.test.auth.entity.ApiClient}, never a
 * {@code User} — a machine caller has no user row.
 */
@Entity
@Table(
        name = "evidence_custody_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_evidence_custody_records", columnNames = {"evidence_file_id", "sequence_no"}),
        indexes = @Index(name = "ix_evidence_custody_records_file", columnList = "evidence_file_id"))
@Getter
@Setter
public class EvidenceCustodyRecord extends BaseEntity {

    @Column(name = "evidence_file_id", nullable = false)
    private Long evidenceFileId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CustodyTransition transition;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_api_client_id")
    private Long actorApiClientId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "checksum_before", length = 64)
    private String checksumBefore;

    /** Must equal checksumBefore for every transition except PURGED — a mismatch means tampering. */
    @Column(name = "checksum_after", length = 64)
    private String checksumAfter;

    @Column(name = "storage_path_before", length = 512)
    private String storagePathBefore;

    @Column(name = "storage_path_after", length = 512)
    private String storagePathAfter;

    @Column(length = 500)
    private String note;
}
