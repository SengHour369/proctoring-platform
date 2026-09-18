package com.example.test.payment.enums;

/** Lifecycle of one payment transaction. */
public enum PaymentTransactionStatus {
    INITIATED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    DISPUTED,
    CHARGEBACK
}
