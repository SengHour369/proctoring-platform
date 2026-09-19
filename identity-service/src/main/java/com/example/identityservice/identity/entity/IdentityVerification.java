package com.example.test.identity.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.identity.enums.VerificationMethod;
import com.example.test.identity.enums.VerificationStatus;
import com.example.test.proctoring.entity.ProctoringSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single identity check on a candidate: face match against the enrolment photo, an ID document
 * scan, or a proctor's own confirmation. Many rows per attempt — a failed match followed by a
 * manual override is two checks, and both belong in the record.
 *
 * <p>{@link ProctoringSession} keeps only the resulting flag; the working — which method, which
 * score, against which photo, judged by whom — lives here, because that is what an appeal or an
 * impersonation investigation actually needs.
 */
@Entity
@Table(
        name = "identity_verifications",
        indexes = {
                @Index(name = "ix_identity_verifications_attempt", columnList = "exam_attempt_id"),
                @Index(name = "ix_identity_verifications_candidate", columnList = "candidate_user_id"),
                @Index(name = "ix_identity_verifications_status", columnList = "status")
        })
@Getter
@Setter
public class IdentityVerification extends BaseEntity {

    @Column(name = "candidate_user_id", nullable = false)
    private Long candidateUserId;

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "proctoring_session_id")
    private Long proctoringSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VerificationMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo = 1;

    /** Similarity score, 0.0000–1.0000, for the biometric methods. */
    @Column(name = "match_score", precision = 5, scale = 4)
    private BigDecimal matchScore;

    /** Threshold in force at the time — a later retune must not change this verdict's meaning. */
    @Column(name = "match_threshold", precision = 5, scale = 4)
    private BigDecimal matchThreshold;

    @Column(name = "liveness_score", precision = 5, scale = 4)
    private BigDecimal livenessScore;

    /** The enrolment photo compared against, as it stood at verification time. */
    @Column(name = "reference_photo_path", length = 512)
    private String referencePhotoPath;

    /** The live capture or document scan used for the comparison. */
    @Column(name = "captured_evidence_id")
    private Long capturedEvidenceId;

    @Column(name = "document_type", length = 48)
    private String documentType;

    /** Last four characters only — the full document number is never stored. */
    @Column(name = "document_last4", length = 8)
    private String documentLast4;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    /** Set for MANUAL_PROCTOR checks and for overrides of a failed automatic one. */
    @Column(name = "verified_by_user_id")
    private Long verifiedByUserId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;
}
