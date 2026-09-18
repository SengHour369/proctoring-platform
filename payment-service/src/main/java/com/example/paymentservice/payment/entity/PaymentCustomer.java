package com.example.paymentservice.payment.entity;

import com.example.paymentservice.common.entity.BaseEntity;
import com.example.paymentservice.payment.enums.PaymentProvider;
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

import java.util.UUID;

/**
 * A candidate's billing profile with one payment processor — the record that has to exist before
 * a card can be tokenized against it, mirroring how these processors actually work (Stripe, for
 * one, requires a Customer object before a PaymentMethod can be attached). One user can have a
 * separate {@code PaymentCustomer} row per provider, which is why {@code (userId, provider)} is
 * unique rather than {@code userId} alone.
 *
 * <p>{@code defaultPaymentCardId} and {@code payment_cards.paymentCustomerId} reference each
 * other, but that's not a problem here the way it would be for a database enforcing FKs inline at
 * insert time: this project's Hibernate-generated tables carry no inline foreign keys at all —
 * every constraint is applied afterward, once by {@code schema-foreign-keys.sql}, after both
 * tables already exist. The same pattern already covers the self-references on
 * {@code RetakeGrant} and {@code ExamWindowOverride}.
 */
@Entity
@Table(
        name = "payment_customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_customers_user_provider", columnNames = {"user_id", "provider"}),
                @UniqueConstraint(name = "uk_payment_customers_provider_customer", columnNames = {"provider", "provider_customer_id"})
        },
        indexes = @Index(name = "ix_payment_customers_user", columnList = "user_id"))
@Getter
@Setter
public class PaymentCustomer extends BaseEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The card this customer's charges default to, when none is specified. */
    @Column(name = "default_payment_card_id")
    private Long defaultPaymentCardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentProvider provider;

    @Column(name = "provider_customer_id", nullable = false, length = 128)
    private String providerCustomerId;

    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    @Column(name = "billing_name", length = 150)
    private String billingName;

    @Column(name = "billing_phone", length = 32)
    private String billingPhone;

    @Column(name = "billing_address_line1", length = 200)
    private String billingAddressLine1;

    @Column(name = "billing_address_line2", length = 200)
    private String billingAddressLine2;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Column(name = "billing_postal_code", length = 32)
    private String billingPostalCode;

    @Column(name = "billing_country", length = 2)
    private String billingCountry;

    @Column(name = "tax_id", length = 64)
    private String taxId;

    /** ISO 4217 alpha code. Plain text, not an enum — the same reasoning as RiskFactorConfig.triggerCode:
     * a currency list is exactly the kind of open-ended vocabulary that shouldn't need a migration to grow. */
    @Column(name = "preferred_currency", length = 3)
    private String preferredCurrency;

    @Column(name = "is_delinquent", nullable = false)
    private boolean delinquent = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;
}
