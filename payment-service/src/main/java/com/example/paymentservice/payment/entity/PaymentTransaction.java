package com.example.paymentservice.payment.entity;

import com.example.paymentservice.common.entity.BaseEntity;
import com.example.paymentservice.payment.enums.PaymentProvider;
import com.example.paymentservice.payment.enums.PaymentTransactionStatus;
import com.example.paymentservice.payment.enums.PaymentTransactionType;
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
import java.util.UUID;

/**
 * One attempt by a processor to move money — an authorization, a capture, a refund, a payout, and
 * so on. {@code parentTransactionId} links a follow-up event back to the transaction it acts on
 * (a REFUND back to the SALE it refunds, a CAPTURE back to its AUTHORIZATION), the same
 * self-reference shape already used by {@code RetakeGrant.supersedesGrantId}.
 *
 * <p>{@code amountMinor}/{@code amountRefundedMinor} are integer minor units (cents), never a
 * floating type — the standard way to avoid rounding error in money math. {@code currency} is a
 * plain ISO-4217 string, matching {@code PaymentCustomer.preferredCurrency}, not an enum.
 *
 * <p>{@code idempotencyKey} follows the same nullable-unique shape as
 * {@code Notification.idempotencyKey}: unique when present, any number of nulls allowed — exactly
 * Postgres's own unique-index semantics, so no partial index is needed to express it.
 */
@Entity
@Table(
        name = "payment_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_transactions_provider_txn", columnNames = {"provider", "provider_transaction_id"}),
                @UniqueConstraint(name = "uk_payment_transactions_idempotency_key", columnNames = {"idempotency_key"})
        },
        indexes = {
                @Index(name = "ix_payment_transactions_customer", columnList = "payment_customer_id, created_at"),
                @Index(name = "ix_payment_transactions_card", columnList = "payment_card_id, created_at"),
                @Index(name = "ix_payment_transactions_status", columnList = "status, created_at"),
                @Index(name = "ix_payment_transactions_parent", columnList = "parent_transaction_id"),
                @Index(name = "ix_payment_transactions_reference", columnList = "reference")
        })
@Getter
@Setter
public class PaymentTransaction extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "payment_customer_id", nullable = false)
    private Long paymentCustomerId;

    @Column(name = "payment_card_id")
    private Long paymentCardId;

    /** The transaction this one acts on — e.g. a REFUND's parent is the SALE/CAPTURE it refunds. */
    @Column(name = "parent_transaction_id")
    private Long parentTransactionId;

    @Column(name = "initiated_by_user_id")
    private Long initiatedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentProvider provider;

    @Column(name = "provider_transaction_id", nullable = false, length = 128)
    private String providerTransactionId;

    @Column(name = "provider_intent_id", length = 128)
    private String providerIntentId;

    /** ISO 4217 alpha code. */
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "amount_refunded_minor", nullable = false)
    private long amountRefundedMinor = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentTransactionStatus status = PaymentTransactionStatus.INITIATED;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(length = 500)
    private String description;

    @Column(name = "statement_descriptor", length = 22)
    private String statementDescriptor;

    /** Free-text correlation key back to the domain object this charge is for (e.g. an exam attempt). */
    @Column(length = 64)
    private String reference;

    /** Caller-supplied key preventing the same charge from being submitted to the processor twice. */
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "three_ds_authenticated", nullable = false)
    private boolean threeDsAuthenticated = false;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;
}
