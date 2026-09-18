package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.exam.dto.ExamWindowOverrideResponse;
import com.example.examservice.exam.dto.GrantWindowOverrideRequest;
import com.example.examservice.exam.entity.ExamAssignment;
import com.example.examservice.exam.entity.ExamWindowOverride;
import com.example.examservice.exam.repository.ExamAssignmentRepository;
import com.example.examservice.exam.repository.ExamWindowOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamWindowOverrideServiceImpl implements ExamWindowOverrideService {

    private final ExamWindowOverrideRepository examWindowOverrideRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public ExamWindowOverrideResponse grantOverride(Long assignmentId, GrantWindowOverrideRequest request,
                                                      Long callerUserId) {
        requirePermission(assignmentId, callerUserId);

        ExamAssignment assignment = loadAssignment(assignmentId);

        if (request.justificationRef() == null || request.justificationRef().isBlank()) {
            throw new InvalidRequestException("justificationRef is required");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new InvalidRequestException("reason is required");
        }
        if (!request.overriddenWindowStartAt().isBefore(request.overriddenWindowEndAt())) {
            throw new InvalidRequestException("overriddenWindowStartAt must be before overriddenWindowEndAt");
        }

        ExamWindowOverride override = new ExamWindowOverride();
        override.setExamAssignmentId(assignment.getId());
        override.setOverriddenWindowStartAt(request.overriddenWindowStartAt());
        override.setOverriddenWindowEndAt(request.overriddenWindowEndAt());
        override.setReason(request.reason());
        override.setJustificationRef(request.justificationRef());
        override.setGrantedByUserId(callerUserId);
        override.setGrantedAt(Instant.now());
        override.setExpiresAt(request.expiresAt());
        override = examWindowOverrideRepository.save(override);

        auditService.recordSuccess(AuditAction.CONFIG_CHANGE, "ExamWindowOverride", override.getId(), callerUserId,
                null, Map.of("examAssignmentId", assignmentId), request.reason());

        return ExamWindowOverrideResponse.from(override);
    }

    @Override
    @Transactional
    public ExamWindowOverrideResponse revoke(Long overrideId, String reason, Long callerUserId) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("reason is required");
        }
        ExamWindowOverride original = examWindowOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new EntityNotFoundException("ExamWindowOverride not found: " + overrideId));

        requirePermission(original.getExamAssignmentId(), callerUserId);

        ExamWindowOverride revocation = new ExamWindowOverride();
        revocation.setExamAssignmentId(original.getExamAssignmentId());
        revocation.setOverriddenWindowStartAt(original.getOverriddenWindowStartAt());
        revocation.setOverriddenWindowEndAt(original.getOverriddenWindowEndAt());
        revocation.setReason(reason);
        revocation.setJustificationRef(original.getJustificationRef());
        revocation.setGrantedByUserId(callerUserId);
        revocation.setGrantedAt(Instant.now());
        revocation.setExpiresAt(Instant.now());
        revocation.setSupersedesOverrideId(original.getId());
        revocation = examWindowOverrideRepository.save(revocation);

        auditService.recordSuccess(AuditAction.CONFIG_CHANGE, "ExamWindowOverride", revocation.getId(), callerUserId,
                Map.of("supersedes", original.getId()), null, reason);

        return ExamWindowOverrideResponse.from(revocation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamWindowOverride> findActiveCoveringOverride(Long examAssignmentId, Instant windowStart,
                                                                     Instant windowEnd) {
        List<ExamWindowOverride> rows =
                examWindowOverrideRepository.findByExamAssignmentIdOrderByGrantedAtDesc(examAssignmentId);

        Set<Long> superseded = new HashSet<>();
        for (ExamWindowOverride row : rows) {
            if (row.getSupersedesOverrideId() != null) {
                superseded.add(row.getSupersedesOverrideId());
            }
        }

        Instant now = Instant.now();
        return rows.stream()
                .filter(row -> !superseded.contains(row.getId()))
                .filter(row -> row.getExpiresAt() == null || row.getExpiresAt().isAfter(now))
                .filter(row -> covers(row, windowStart, windowEnd))
                .findFirst();
    }

    private boolean covers(ExamWindowOverride override, Instant windowStart, Instant windowEnd) {
        boolean coversStart = windowStart == null || !windowStart.isBefore(override.getOverriddenWindowStartAt());
        boolean coversEnd = windowEnd == null || !windowEnd.isAfter(override.getOverriddenWindowEndAt());
        return coversStart && coversEnd;
    }

    private ExamAssignment loadAssignment(Long assignmentId) {
        return examAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("ExamAssignment not found: " + assignmentId));
    }

    private void requirePermission(Long assignmentId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, Permissions.EXAM_ASSIGN)) {
            auditService.recordDenied(AuditAction.CONFIG_CHANGE, "ExamWindowOverride", assignmentId, callerUserId,
                    "missing " + Permissions.EXAM_ASSIGN);
            throw new AccessDeniedException("missing permission: " + Permissions.EXAM_ASSIGN);
        }
    }
}
