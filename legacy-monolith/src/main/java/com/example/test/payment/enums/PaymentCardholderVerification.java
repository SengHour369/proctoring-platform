package com.example.test.payment.enums;

/** Result of one cardholder-verification check (CVV or AVS) at the processor. */
public enum PaymentCardholderVerification {
    NOT_ATTEMPTED,
    PASSED,
    FAILED,
    UNAVAILABLE,
    UNRECOGNIZED
}
