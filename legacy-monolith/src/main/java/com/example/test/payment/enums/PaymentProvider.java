package com.example.test.payment.enums;

/** Which PCI-compliant processor tokenized a stored card or produced a transaction. */
public enum PaymentProvider {
    STRIPE,
    ADYEN,
    BRAINTREE,
    SQUARE,
    PAYPAL,
    MANUAL
}
