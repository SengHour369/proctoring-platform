package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.proctoring.enums.EvidenceAccessAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Who looked at which piece of evidence, when, and why. Webcam footage of a person's home is the
 * most sensitive data this system holds, so every read is logged — not just every change.
 *
 * <p>Separate from AUDIT_LOGS because it is written on a high-traffic path (a reviewer scrubbing a
 * recording), is queried per evidence file when answering a subject-access request, and has its
 * own, longer retention than ordinary audit rows.
 */
@Entity
@Table(
        name = "evidence_access_logs",
        indexes = {
                @Index(name = "ix_evidence_access_file", columnList = "evidence_file_id, accessed_at"),
                @Index(name = "ix_evidence_access_actor", columnList = "actor_user_id, accessed_at")
        })
@Getter
@Setter
public class EvidenceAccessLog extends BaseEntity {

    @Column(name = "evidence_file_id", nullable = false)
    private Long evidenceFileId;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvidenceAccessAction action;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt = Instant.now();

    /** The case or ticket the access was made under; unattributed access is a policy breach. */
    @Column(name = "context_reference", length = 64)
    private String contextReference;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "was_granted", nullable = false)
    private boolean granted = true;

    @Column(name = "denied_reason", length = 255)
    private String deniedReason;
}
