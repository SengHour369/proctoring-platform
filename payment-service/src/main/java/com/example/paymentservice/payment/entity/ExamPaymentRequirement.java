package com.example.paymentservice.payment.entity;

import com.example.paymentservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A fee an exam requires before a candidate can start it — one row per rule, same shape as
 * {@code ExamPrerequisite}: a flat {@code active} flag rather than a status enum, since there is
 * only "in force" or "retired" and no in-between workflow. No {@code publicId} either, matching
 * {@code ExamPrerequisite}'s own lack of one — this row is never referenced by anything outside
 * this database.
 */
@Entity
@Table(
        name = "exam_payment_requirements",
        indexes = @Index(name = "ix_exam_payment_requirements_exam", columnList = "exam_id"))
@Getter
@Setter
public class ExamPaymentRequirement extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    /** ISO 4217 alpha code. */
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
