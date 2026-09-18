package com.example.test.payment.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.payment.enums.CardBrand;
import com.example.test.payment.enums.PaymentCardFunding;
import com.example.test.payment.enums.PaymentCardStatus;
import com.example.test.payment.enums.PaymentCardholderVerification;
import com.example.test.payment.enums.PaymentProvider;
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
 * A candidate's payment card on file, for exam fees — tokenized metadata only. The actual card
 * number and CVV never reach this database: they're handed straight to a PCI-compliant processor
 * (Stripe, Adyen, ...), and this row stores only what that processor hands back —
 * {@code providerPaymentMethodId}, an opaque token — plus the display and risk-signal fields
 * (brand, last four, expiry, fingerprint, verification results) needed to show the candidate
 * which card is on file and to reason about it without ever touching the real number. Same
 * reasoning as {@code User.passwordHash} and {@code ApiClient.clientSecretHash}: never store the
 * secret, only a reference to it.
 *
 * <p>Both {@code paymentCustomerId} and {@code userId} are stored, even though the user is
 * reachable through the customer — the same denormalization {@code ExamResult} already does for
 * {@code examId}/{@code candidateUserId}, so listing "this candidate's cards" doesn't need a join.
 *
 * <p>"At most one default card per customer" is enforced by {@code PaymentCardService.setDefault}
 * (unset every other card, then set this one), not by a database constraint — Hibernate's schema
 * generation, which is this project's actual source of DDL, has no annotation for a partial
 * unique index, and hand-writing one here would just go stale the next time the schema
 * regenerates.
 */
@Entity
@Table(
        name = "payment_cards",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_cards_provider_method",
                columnNames = {"provider", "provider_payment_method_id"}),
        indexes = {
                @Index(name = "ix_payment_cards_customer", columnList = "payment_customer_id, status"),
                @Index(name = "ix_payment_cards_user", columnList = "user_id, status"),
                @Index(name = "ix_payment_cards_fingerprint", columnList = "fingerprint")
        })
@Getter
@Setter
public class PaymentCard extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "payment_customer_id", nullable = false)
    private Long paymentCustomerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentProvider provider;

    /** The processor's token for this specific card — never the card number itself. */
    @Column(name = "provider_payment_method_id", nullable = false, length = 128)
    private String providerPaymentMethodId;

    /** Legacy/provider-specific: some processors issue a raw card token distinct from the payment method id. */
    @Column(name = "provider_card_token", length = 128)
    private String providerCardToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CardBrand brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentCardFunding funding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentCardStatus status = PaymentCardStatus.PENDING_VERIFICATION;

    @Column(nullable = false, length = 4)
    private String last4;

    /** Bank identification number — the first 6-8 digits. Routing metadata, not sensitive like a full PAN. */
    @Column(length = 8)
    private String bin;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Column(name = "cardholder_name", length = 150)
    private String cardholderName;

    @Column(length = 150)
    private String issuer;

    @Column(name = "issuer_country", length = 2)
    private String issuerCountry;

    @Column(name = "billing_country", length = 2)
    private String billingCountry;

    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    /** Lets duplicate-card detection run across accounts without ever exposing the PAN. */
    @Column(length = 128)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "cvv_check", nullable = false, length = 16)
    private PaymentCardholderVerification cvvCheck = PaymentCardholderVerification.NOT_ATTEMPTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "avs_line1_check", nullable = false, length = 16)
    private PaymentCardholderVerification avsLine1Check = PaymentCardholderVerification.NOT_ATTEMPTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "avs_postal_code_check", nullable = false, length = 16)
    private PaymentCardholderVerification avsPostalCodeCheck = PaymentCardholderVerification.NOT_ATTEMPTED;

    @Column(name = "three_ds_supported", nullable = false)
    private boolean threeDsSupported = false;

    @Column(name = "three_ds_enrolled", nullable = false)
    private boolean threeDsEnrolled = false;

    /** The card charged by default when this candidate isn't asked to pick one. */
    @Column(name = "is_default", nullable = false)
    private boolean defaultCard = false;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** Set on any transition to REVOKED — a candidate removing it and an admin invalidating it both land here. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;
}
