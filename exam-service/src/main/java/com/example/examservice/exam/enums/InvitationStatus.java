package com.example.examservice.exam.enums;

/** Delivery and acceptance state of an exam invitation. */
public enum InvitationStatus {
    PENDING,
    SENT,
    DELIVERED,
    OPENED,
    ACCEPTED,
    EXPIRED,
    CANCELLED,
    FAILED
}
