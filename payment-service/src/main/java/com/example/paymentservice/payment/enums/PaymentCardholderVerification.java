package com.example.paymentservice.payment.enums;

/** Result of one cardholder-verification check (CVV or AVS) at the processor. */
public enum PaymentCardholderVerification {
    NOT_ATTEMPTED,
    PASSED,
    FAILED,
    UNAVAILABLE,
    UNRECOGNIZED
}
