package com.example.paymentservice.payment.enums;

/**
 * Lifecycle of one stored payment card. No DECLINED here — a decline is an outcome of one
 * {@code PaymentTransaction}, not a property of the card itself; a card that's been declined
 * once can still be ACTIVE for the next attempt.
 */
public enum PaymentCardStatus {
    ACTIVE,
    EXPIRED,
    PENDING_VERIFICATION,
    SUSPENDED,
    REVOKED
}
