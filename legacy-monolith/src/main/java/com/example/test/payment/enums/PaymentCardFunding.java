package com.example.test.payment.enums;

/** How the card draws funds, as reported by the processor. */
public enum PaymentCardFunding {
    CREDIT,
    DEBIT,
    PREPAID,
    CHARGE,
    UNKNOWN
}
