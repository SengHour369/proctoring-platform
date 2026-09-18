package com.example.notificationservice.notification.entity;

import com.example.notificationservice.common.entity.BaseEntity;
import com.example.notificationservice.notification.enums.NotificationChannel;
import com.example.notificationservice.notification.enums.NotificationType;
import com.example.notificationservice.notification.enums.SuppressionKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * A rule that defers, throttles, or blocks a class of outgoing notification — quiet hours in the
 * recipient's own time zone, an opt-out, a rate limit, or a duplicate-content window.
 *
 * <p>A duplicate-window suppression is distinct from {@code Notification.idempotencyKey}: the
 * key stops one retried trigger from sending twice; this stops twenty *different* triggers (a
 * candidate failing twenty exams in a day) from producing twenty identical emails. A suppressed
 * send is deferred, not dropped — the {@code Notification} row still gets written, just with a
 * later {@code scheduledFor} or a {@code CANCELLED} status that says why.
 */
@Entity
@Table(
        name = "notification_suppressions",
        indexes = @Index(name = "ix_notification_suppressions_recipient", columnList = "recipient_user_id"))
@Getter
@Setter
public class NotificationSuppression extends BaseEntity {

    /** Null for a rule that applies to every recipient. */
    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    /** Null for a rule that isn't specific to one notification type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 32)
    private NotificationType notificationType;

    /** Null for a rule that applies regardless of channel. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "suppression_kind", nullable = false, length = 24)
    private SuppressionKind suppressionKind;

    /** For QUIET_HOURS, in the recipient's own User.timeZone — never the server's. */
    @Column(name = "window_start_local")
    private LocalTime windowStartLocal;

    @Column(name = "window_end_local")
    private LocalTime windowEndLocal;

    @Column(name = "max_per_window")
    private Integer maxPerWindow;

    @Column(name = "window_seconds")
    private Integer windowSeconds;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
