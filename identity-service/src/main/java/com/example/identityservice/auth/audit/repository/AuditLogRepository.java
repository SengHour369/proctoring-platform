package com.example.identityservice.auth.audit.repository;

import com.example.identityservice.auth.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActorUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            Long actorUserId, Instant from, Instant to, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            String entityType, Long entityId, Instant from, Instant to, Pageable pageable);
}
