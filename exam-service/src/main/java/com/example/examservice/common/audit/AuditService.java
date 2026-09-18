package com.example.examservice.common.audit;

import com.example.examservice.common.entity.AuditLog;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.enums.AuditOutcome;
import com.example.examservice.common.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Writes {@link AuditLog} rows for every consequential action. A denied permission check and a
 * completed change are both recorded here — the log must show attempts, not just outcomes.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void recordSuccess(AuditAction action, String entityType, Long entityId, Long actorUserId,
                               Object beforeState, Object afterState, String reason) {
        record(action, entityType, entityId, actorUserId, AuditOutcome.SUCCESS, beforeState, afterState, reason);
    }

    public void recordDenied(AuditAction action, String entityType, Long entityId, Long actorUserId, String reason) {
        record(action, entityType, entityId, actorUserId, AuditOutcome.DENIED, null, null, reason);
    }

    private void record(AuditAction action, String entityType, Long entityId, Long actorUserId,
                         AuditOutcome outcome, Object beforeState, Object afterState, String reason) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setActorUserId(actorUserId);
        log.setOutcome(outcome);
        log.setReason(reason);
        log.setBeforeState(toJson(beforeState));
        log.setAfterState(toJson(afterState));
        auditLogRepository.save(log);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
