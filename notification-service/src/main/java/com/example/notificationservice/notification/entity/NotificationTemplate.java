package com.example.notificationservice.notification.entity;

import com.example.notificationservice.common.entity.BaseEntity;
import com.example.notificationservice.notification.enums.NotificationChannel;
import com.example.notificationservice.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Editable subject and body for one (type, channel, locale) combination. In the database rather
 * than in resource bundles so an administrator can correct the wording of an exam reminder without
 * a release — and, given the platform sends in more than one language, so the Khmer and English
 * versions of a message stay side by side.
 */
@Entity
@Table(
        name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_templates",
                columnNames = {"notification_type", "channel", "locale"}))
@Getter
@Setter
public class NotificationTemplate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel = NotificationChannel.EMAIL;

    @Column(nullable = false, length = 8)
    private String locale = "en";

    @Column(name = "subject_template", length = 255)
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, length = 8000)
    private String bodyTemplate;

    /** Placeholders the template expects, e.g. {@code candidateName,examTitle,startsAt}. */
    @Column(name = "expected_variables", length = 500)
    private String expectedVariables;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "version", nullable = false)
    private int version = 1;
}
