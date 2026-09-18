package com.example.identityservice.auth.audit;

import com.example.identityservice.auth.audit.entity.AuditLog;
import com.example.identityservice.auth.audit.enums.AuditAction;
import com.example.identityservice.auth.audit.enums.AuditOutcome;
import com.example.identityservice.auth.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Writes {@link AuditLog} rows.
 *
 * <p>{@link #writeOutcome} runs in its own transaction: a denial or a failure is recorded on the
 * way to rejecting the call, and the rejection rolls the caller's transaction back. Without the
 * separate transaction the evidence that someone was denied would roll back with it.
 */
@Component
public class AuditWriter {

    private final AuditLogRepository auditLogRepository;

    public AuditWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Records a successful action as part of the caller's transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog write(
            Long actorUserId,
            AuditAction action,
            String entityType,
            Long entityId,
            String beforeState,
            String afterState,
            String reason) {
        return auditLogRepository.save(
                build(actorUserId, action, AuditOutcome.SUCCESS, entityType, entityId, beforeState, afterState, reason));
    }

    /** Records a denied or failed action in a transaction of its own, so it survives the rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog writeOutcome(
            Long actorUserId,
            AuditAction action,
            AuditOutcome outcome,
            String entityType,
            Long entityId,
            String reason) {
        return auditLogRepository.save(
                build(actorUserId, action, outcome, entityType, entityId, null, null, reason));
    }

    private AuditLog build(
            Long actorUserId,
            AuditAction action,
            AuditOutcome outcome,
            String entityType,
            Long entityId,
            String beforeState,
            String afterState,
            String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setOutcome(outcome);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOccurredAt(Instant.now());
        auditLog.setBeforeState(beforeState);
        auditLog.setAfterState(afterState);
        auditLog.setReason(reason);
        return auditLog;
    }
}
