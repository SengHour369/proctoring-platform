package com.example.identityservice.auth.audit.entity;

import com.example.identityservice.auth.audit.enums.AuditAction;
import com.example.identityservice.auth.audit.enums.AuditOutcome;
import com.example.identityservice.common.entity.BaseEntity;
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
 * Append-only record of every consequential identity-module action: who did what to which
 * object, and whether it succeeded. Rows are never updated or deleted.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "ix_id_audit_logs_actor", columnList = "actor_user_id, occurred_at"),
                @Index(name = "ix_id_audit_logs_entity", columnList = "entity_type, entity_id")
        })
@Getter
@Setter
public class AuditLog extends BaseEntity {

    /** Null for actions taken by the platform itself. */
    @Column(name = "actor_user_id")
    private Long actorUserId;

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

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private String afterState;

    /** Justification, required by policy for sensitive changes such as role deletion. */
    @Column(length = 1000)
    private String reason;
}
