package com.example.examservice.common.entity;

import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.enums.AuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Immutable record of a consequential action against an exam-service entity: who did what, to
 * which row, and whether it was allowed. Written by every lifecycle transition, structural edit,
 * and permission check in this module — never updated once inserted.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "ix_audit_logs_entity", columnList = "entity_type, entity_id"),
                @Index(name = "ix_audit_logs_actor", columnList = "actor_user_id")
        })
@Getter
@Setter
public class AuditLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditAction action;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditOutcome outcome = AuditOutcome.SUCCESS;

    @Column(length = 500)
    private String reason;

    @Column(name = "before_state", columnDefinition = "text")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "text")
    private String afterState;
}
