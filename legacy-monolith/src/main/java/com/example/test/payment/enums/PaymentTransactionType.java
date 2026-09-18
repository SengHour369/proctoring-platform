package com.example.test.payment.enums;

/** What kind of money movement one PaymentTransaction row represents. */
public enum PaymentTransactionType {
    AUTHORIZATION,
    CAPTURE,
    SALE,
    REFUND,
    VOID,
    CHARGEBACK,
    PAYOUT
}
