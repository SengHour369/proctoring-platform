package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.exam.dto.GrantRetakeRequest;
import com.example.examservice.exam.dto.RetakeGrantResponse;
import com.example.examservice.exam.entity.ExamAssignment;
import com.example.examservice.exam.entity.RetakeGrant;
import com.example.examservice.exam.port.ReviewDecisionPort;
import com.example.examservice.exam.repository.ExamAssignmentRepository;
import com.example.examservice.exam.repository.RetakeGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RetakeGrantServiceImpl implements RetakeGrantService {

    private final RetakeGrantRepository retakeGrantRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final ReviewDecisionPort reviewDecisionPort;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public RetakeGrantResponse grantRetake(Long assignmentId, GrantRetakeRequest request, Long callerUserId) {
        requirePermission(assignmentId, callerUserId);

        ExamAssignment assignment = loadAssignment(assignmentId);

        if (request.reason() == null || request.reason().isBlank()) {
            throw new InvalidRequestException("reason is required");
        }
        if (request.additionalAttempts() < 1) {
            throw new InvalidRequestException("additionalAttempts must be >= 1");
        }
        if (!reviewDecisionPort.isGrantRetakeDecision(request.reviewDecisionId())) {
            throw new InvalidRequestException("reviewDecisionId must reference a GRANT_RETAKE decision");
        }

        RetakeGrant grant = new RetakeGrant();
        grant.setExamAssignmentId(assignment.getId());
        grant.setCandidateUserId(assignment.getCandidateUserId());
        grant.setReviewDecisionId(request.reviewDecisionId());
        grant.setGrantedByUserId(callerUserId);
        grant.setGrantedAt(Instant.now());
        grant.setAdditionalAttempts(request.additionalAttempts());
        grant.setExpiresAt(request.expiresAt());
        grant.setReason(request.reason());
        grant = retakeGrantRepository.save(grant);

        auditService.recordSuccess(AuditAction.PERMISSION_CHANGE, "RetakeGrant", grant.getId(), callerUserId,
                null, Map.of("examAssignmentId", assignmentId, "additionalAttempts", request.additionalAttempts()),
                request.reason());

        return RetakeGrantResponse.from(grant);
    }

    @Override
    @Transactional
    public void consume(Long grantId, Long attemptId) {
        RetakeGrant grant = retakeGrantRepository.findById(grantId)
                .orElseThrow(() -> new EntityNotFoundException("RetakeGrant not found: " + grantId));
        if (grant.getConsumedAt() != null) {
            throw new InvalidRequestException("retake grant already consumed");
        }
        if (grant.getExpiresAt() != null && !grant.getExpiresAt().isAfter(Instant.now())) {
            throw new InvalidRequestException("retake grant has expired");
        }
        grant.setConsumedAt(Instant.now());
        grant.setConsumedByAttemptId(attemptId);
        retakeGrantRepository.save(grant);
    }

    @Override
    @Transactional
    public RetakeGrantResponse revoke(Long grantId, String reason, Long callerUserId) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("reason is required");
        }
        RetakeGrant original = retakeGrantRepository.findById(grantId)
                .orElseThrow(() -> new EntityNotFoundException("RetakeGrant not found: " + grantId));

        requirePermission(original.getExamAssignmentId(), callerUserId);

        RetakeGrant revocation = new RetakeGrant();
        revocation.setExamAssignmentId(original.getExamAssignmentId());
        revocation.setCandidateUserId(original.getCandidateUserId());
        revocation.setReviewDecisionId(original.getReviewDecisionId());
        revocation.setGrantedByUserId(callerUserId);
        revocation.setGrantedAt(Instant.now());
        revocation.setAdditionalAttempts(0);
        revocation.setReason(reason);
        revocation.setSupersedesGrantId(original.getId());
        revocation = retakeGrantRepository.save(revocation);

        auditService.recordSuccess(AuditAction.PERMISSION_CHANGE, "RetakeGrant", revocation.getId(), callerUserId,
                Map.of("supersedes", original.getId()), null, reason);

        return RetakeGrantResponse.from(revocation);
    }

    @Override
    @Transactional(readOnly = true)
    public int computeEffectiveCap(Long examAssignmentId, int baseCap) {
        List<RetakeGrant> grants = retakeGrantRepository.findByExamAssignmentId(examAssignmentId);

        Set<Long> superseded = new HashSet<>();
        for (RetakeGrant grant : grants) {
            if (grant.getSupersedesGrantId() != null) {
                superseded.add(grant.getSupersedesGrantId());
            }
        }

        Instant now = Instant.now();
        int extra = grants.stream()
                .filter(g -> !superseded.contains(g.getId()))
                .filter(g -> g.getConsumedAt() == null)
                .filter(g -> g.getExpiresAt() == null || g.getExpiresAt().isAfter(now))
                .mapToInt(RetakeGrant::getAdditionalAttempts)
                .sum();

        return baseCap + extra;
    }

    private ExamAssignment loadAssignment(Long assignmentId) {
        return examAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("ExamAssignment not found: " + assignmentId));
    }

    private void requirePermission(Long assignmentId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, Permissions.EXAM_GRANT_EXTRA_ATTEMPTS)) {
            auditService.recordDenied(AuditAction.PERMISSION_CHANGE, "RetakeGrant", assignmentId, callerUserId,
                    "missing " + Permissions.EXAM_GRANT_EXTRA_ATTEMPTS);
            throw new AccessDeniedException("missing permission: " + Permissions.EXAM_GRANT_EXTRA_ATTEMPTS);
        }
    }
}
