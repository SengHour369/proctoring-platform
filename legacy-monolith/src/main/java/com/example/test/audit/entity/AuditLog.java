package com.example.test.audit.entity;

import com.example.test.audit.enums.AuditAction;
import com.example.test.audit.enums.AuditOutcome;
import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Append-only record of every consequential action: who did what to which object, from where, and
 * whether it succeeded. In an exam system the interesting rows are the administrative ones — a
 * score overridden, an attempt reinstated, a permission granted, an exam's rules relaxed an hour
 * before it opened.
 *
 * <p>Generic by design ({@code entityType} + {@code entityId} + before/after JSON): a per-table
 * history would multiply the schema and still miss the cross-cutting question "what did this
 * administrator touch last Tuesday?". Rows are never updated or deleted.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "ix_audit_logs_actor", columnList = "actor_user_id, occurred_at"),
                @Index(name = "ix_audit_logs_entity", columnList = "entity_type, entity_id"),
                @Index(name = "ix_audit_logs_action", columnList = "action, occurred_at")
        })
@Getter
@Setter
public class AuditLog extends BaseEntity {

    /** Null for actions taken by the platform itself (schedulers, auto-submit, auto-termination). */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    /** Actor's role at the time — role grants change, and the row must still read correctly. */
    @Column(name = "actor_role", length = 48)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditOutcome outcome = AuditOutcome.SUCCESS;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_label", length = 255)
    private String entityLabel;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private String afterState;

    /** Justification, required by policy for overrides and terminations. */
    @Column(length = 1000)
    private String reason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Correlation id of the originating request, to tie a row to application logs. */
    @Column(name = "request_id", length = 64)
    private String requestId;
}
