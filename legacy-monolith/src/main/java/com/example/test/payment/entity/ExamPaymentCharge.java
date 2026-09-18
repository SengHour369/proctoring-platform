package com.example.test.payment.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.payment.enums.PaymentTransactionStatus;
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
 * One candidate's obligation to pay a specific {@link ExamPaymentRequirement}, and the record of
 * whether they have. {@code amountMinor}/{@code currency} are copied from the requirement at
 * charge-creation time rather than read live from it — the same frozen-snapshot reasoning as
 * {@code RiskAssessment.thresholdVersion}: if the fee changes later, past charges must keep
 * showing what the candidate actually owed at the time.
 *
 * <p>{@code status} reuses {@link PaymentTransactionStatus} rather than introducing a parallel
 * enum — a charge's lifecycle (initiated, paid, refunded, failed...) is the same shape a
 * transaction's is, and the two are kept in step by whichever {@code PaymentTransaction} settles
 * this charge.
 *
 * <p>{@code examAttemptId} is nullable-unique: "at most one charge per attempt" when a charge
 * exists at all, unlimited nulls before an attempt is created — the same pattern as
 * {@code ExamInvitation.tokenHash}.
 */
@Entity
@Table(
        name = "exam_payment_charges",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_payment_charges_attempt", columnNames = {"exam_attempt_id"}),
        indexes = {
                @Index(name = "ix_exam_payment_charges_candidate", columnList = "candidate_user_id, status"),
                @Index(name = "ix_exam_payment_charges_exam", columnList = "exam_id, status")
        })
@Getter
@Setter
public class ExamPaymentCharge extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "exam_attempt_id")
    private Long examAttemptId;

    @Column(name = "exam_assignment_id")
    private Long examAssignmentId;

    @Column(name = "exam_payment_requirement_id", nullable = false)
    private Long examPaymentRequirementId;

    @Column(name = "payment_transaction_id")
    private Long paymentTransactionId;

    @Column(name = "candidate_user_id", nullable = false)
    private Long candidateUserId;

    /** ISO 4217 alpha code, frozen from the requirement at charge-creation time. */
    @Column(nullable = false, length = 3)
    private String currency;

    /** Frozen from the requirement at charge-creation time — a later fee change must not reprice this charge. */
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentTransactionStatus status = PaymentTransactionStatus.INITIATED;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;
}
