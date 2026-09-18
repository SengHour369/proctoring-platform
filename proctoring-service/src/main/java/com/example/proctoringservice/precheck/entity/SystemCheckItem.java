package com.example.proctoringservice.precheck.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.precheck.enums.CheckResult;
import com.example.proctoringservice.precheck.enums.SystemCheckType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Result of one item within a pre-flight run — camera, microphone, bandwidth, screen permission.
 * A row per item rather than a column per item on the run, so the check catalogue can grow without
 * a migration and each item keeps its own measurement.
 */
@Entity
@Table(
        name = "system_check_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_system_check_items", columnNames = {"system_check_id", "check_type"}))
@Getter
@Setter
public class SystemCheckItem extends BaseEntity {

    @Column(name = "system_check_id", nullable = false)
    private Long systemCheckId;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 32)
    private SystemCheckType checkType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CheckResult result = CheckResult.NOT_RUN;

    /** Whether the exam policy makes this item blocking; a WARNING on an optional item is fine. */
    @Column(name = "is_required", nullable = false)
    private boolean required = true;

    @Column(name = "checked_at")
    private Instant checkedAt;

    /** Human-readable outcome shown to the candidate, e.g. "No camera permission granted". */
    @Column(length = 500)
    private String message;

    /** Measured detail — device labels, resolution, sample rate, packet loss. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "measurement")
    private String measurement;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;
}
