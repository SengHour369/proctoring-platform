package com.example.test.config.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.config.enums.SettingCategory;
import com.example.test.config.enums.SettingValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Runtime configuration an administrator can change without a deploy: storage limits, retention
 * periods, notification defaults, AI frame rates, security policy.
 *
 * <p>One typed key-value table rather than a table per area. The trade-off is deliberate — values
 * arrive as strings and must be parsed per {@code valueType} — and it is worth it for settings
 * that are read occasionally and edited by hand. Anything an exam depends on per-sitting is not
 * here: that lives in the exam's own {@code ProctoringPolicy}, so changing a global default cannot
 * alter the rules of an exam already in progress.
 *
 * <p>{@code secret} marks values that must never be returned to a UI or written to a log.
 */
@Entity
@Table(
        name = "system_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_system_settings_key", columnNames = "setting_key"),
        indexes = @Index(name = "ix_system_settings_category", columnList = "category"))
@Getter
@Setter
public class SystemSetting extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SettingCategory category = SettingCategory.GENERAL;

    @Column(name = "setting_key", nullable = false, length = 128)
    private String settingKey;

    @Column(name = "setting_value", length = 4000)
    private String settingValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 16)
    private SettingValueType valueType = SettingValueType.STRING;

    /** Shipped default, so "reset to default" needs no code lookup. */
    @Column(name = "default_value", length = 4000)
    private String defaultValue;

    @Column(length = 500)
    private String description;

    /** Allowed values or bounds, checked before a save. */
    @Column(name = "validation_rule", length = 255)
    private String validationRule;

    @Column(name = "is_secret", nullable = false)
    private boolean secret = false;

    /** True when the change only takes effect after a restart — worth telling the operator. */
    @Column(name = "requires_restart", nullable = false)
    private boolean requiresRestart = false;

    @Column(name = "is_editable", nullable = false)
    private boolean editable = true;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "last_changed_at")
    private Instant lastChangedAt;
}
