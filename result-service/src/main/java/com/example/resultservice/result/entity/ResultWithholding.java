package com.example.resultservice.result.entity;

import com.example.resultservice.common.entity.BaseEntity;
import com.example.resultservice.result.enums.WithholdingReason;
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
 * Why one result is sitting at {@code ExamResult.status = WITHHELD}, for how long, and who
 * eventually released it. Leaving this implicit in the status column is exactly the part most
 * likely to be challenged on appeal — "why was my result held for three weeks?" needs an answer
 * more specific than a status enum.
 *
 * <p>{@code thresholdVersion} is copied at withholding time so a later retune of the risk bands
 * can't reinterpret why an old result was held.
 */
@Entity
@Table(
        name = "result_withholdings",
        indexes = @Index(name = "ix_result_withholdings_result", columnList = "exam_result_id, released_at"))
@Getter
@Setter
public class ResultWithholding extends BaseEntity {

    @Column(name = "exam_result_id", nullable = false)
    private Long examResultId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private WithholdingReason reason;

    @Column(name = "review_case_id")
    private Long reviewCaseId;

    /** The assessment version whose band triggered the withholding, when risk-driven. */
    @Column(name = "risk_assessment_id")
    private Long riskAssessmentId;

    @Column(name = "threshold_version", length = 32)
    private String thresholdVersion;

    @Column(name = "withheld_at", nullable = false)
    private Instant withheldAt = Instant.now();

    /** Null for automatic (threshold-driven) withholding. */
    @Column(name = "withheld_by_user_id")
    private Long withheldByUserId;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by_user_id")
    private Long releasedByUserId;

    /** Required whenever releasedByUserId is set — a release without a reason isn't defensible. */
    @Column(name = "release_reason", length = 1000)
    private String releaseReason;
}
