package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.proctoring.enums.DeviceTrustLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Running tally of one device fingerprint's sightings within one exam. A single device sat by
 * several distinct candidates in the same exam window is a stronger integrity signal than
 * anything one {@link DeviceSession} row alone can show — it only emerges by looking across
 * attempts, which is exactly what a per-session table can't do.
 *
 * <p>This record only ever feeds a risk factor ({@code RiskFactorConfig.triggerKind =
 * DEVICE_SIGNAL}); it never auto-terminates anything on its own, and {@code SHARED_CONFIRMED} is
 * reached only through a human {@code ReviewFinding}, not by this table alone.
 */
@Entity
@Table(
        name = "device_trust_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_trust_records", columnNames = {"device_fingerprint", "exam_id"}),
        indexes = @Index(name = "ix_device_trust_records_level", columnList = "trust_level"))
@Getter
@Setter
public class DeviceTrustRecord extends BaseEntity {

    @Column(name = "device_fingerprint", nullable = false, length = 128)
    private String deviceFingerprint;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "sighting_count", nullable = false)
    private int sightingCount = 1;

    @Column(name = "distinct_candidate_count", nullable = false)
    private int distinctCandidateCount = 1;

    /** JSON list of User.id values — an id, never a name or photo. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "distinct_candidate_ids")
    private String distinctCandidateIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", nullable = false, length = 24)
    private DeviceTrustLevel trustLevel = DeviceTrustLevel.UNKNOWN;

    @Column(name = "flagged_at")
    private Instant flaggedAt;

    @Column(name = "flagged_reason", length = 500)
    private String flaggedReason;
}
