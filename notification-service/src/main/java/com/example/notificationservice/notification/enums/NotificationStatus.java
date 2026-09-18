package com.example.notificationservice.notification.enums;

/** Delivery state of a single notification. */
public enum NotificationStatus {
    PENDING,
    QUEUED,
    SENT,
    DELIVERED,
    OPENED,
    FAILED,
    CANCELLED
}
