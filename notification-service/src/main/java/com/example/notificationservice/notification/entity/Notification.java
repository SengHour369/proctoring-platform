package com.example.notificationservice.notification.entity;

import com.example.notificationservice.common.entity.BaseEntity;
import com.example.notificationservice.notification.enums.NotificationChannel;
import com.example.notificationservice.notification.enums.NotificationStatus;
import com.example.notificationservice.notification.enums.NotificationType;
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

import java.time.Instant;

/**
 * One message to one recipient on one channel — the outbox. Rendered content is stored, not
 * re-derived: the template it came from will be edited, and "what were they actually told, and
 * when?" is a question that gets asked about exam notifications.
 *
 * <p>The subject of the message is addressed by {@code relatedEntityType}/{@code relatedEntityId}
 * rather than a foreign key per business object, since a notification can be about an exam, an
 * attempt, a review case or a result, and the set will keep growing.
 */
@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notifications_idempotency", columnNames = "idempotency_key"),
        indexes = {
                @Index(name = "ix_notifications_recipient", columnList = "recipient_user_id, created_at"),
                @Index(name = "ix_notifications_status", columnList = "status, next_retry_at"),
                @Index(name = "ix_notifications_related", columnList = "related_entity_type, related_entity_id")
        })
@Getter
@Setter
public class Notification extends BaseEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel = NotificationChannel.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "template_id")
    private Long templateId;

    /** Address actually used, kept because a profile change must not rewrite delivery history. */
    @Column(name = "sent_to", nullable = false, length = 255)
    private String sentTo;

    @Column(length = 255)
    private String subject;

    @Column(nullable = false, length = 8000)
    private String body;

    /** Values interpolated into the template, for support and for a resend. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables")
    private String variables;

    @Column(name = "related_entity_type", length = 48)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Provider message id, for reconciling bounces against the outbox. */
    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    /** Guards against duplicate sends when a trigger is retried. */
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;
}
