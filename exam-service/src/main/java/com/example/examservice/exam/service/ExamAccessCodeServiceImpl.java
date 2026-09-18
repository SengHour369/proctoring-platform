package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.PerKeyRateLimiter;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.common.security.TokenHasher;
import com.example.examservice.exam.dto.AccessCodeResponse;
import com.example.examservice.exam.dto.ValidateAccessCodeResponse;
import com.example.examservice.exam.entity.ExamAssignment;
import com.example.examservice.exam.enums.AssignmentStatus;
import com.example.examservice.exam.repository.ExamAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamAccessCodeServiceImpl implements ExamAccessCodeService {

    private static final Set<AssignmentStatus> GENERATABLE_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.NOTIFIED);

    private static final Set<AssignmentStatus> VALIDATABLE_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.NOTIFIED, AssignmentStatus.STARTED);

    private static final int ACCESS_CODE_LENGTH = 10;
    private static final int MAX_FAILED_VALIDATIONS = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

    private final ExamAssignmentRepository examAssignmentRepository;
    private final TokenHasher tokenHasher;
    private final PerKeyRateLimiter rateLimiter;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public AccessCodeResponse generateCode(Long assignmentId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, Permissions.EXAM_GENERATE_ACCESS_CODE)) {
            auditService.recordDenied(AuditAction.UPDATE, "ExamAssignment", assignmentId, callerUserId,
                    "missing " + Permissions.EXAM_GENERATE_ACCESS_CODE);
            throw new AccessDeniedException("missing permission: " + Permissions.EXAM_GENERATE_ACCESS_CODE);
        }

        ExamAssignment assignment = loadAssignment(assignmentId);
        if (!GENERATABLE_STATUSES.contains(assignment.getStatus())) {
            throw new InvalidRequestException("assignment is not in a code-generatable status");
        }

        String code = tokenHasher.generateAccessCode(ACCESS_CODE_LENGTH);
        assignment.setAccessCodeHash(tokenHasher.hash(code));
        examAssignmentRepository.save(assignment);
        rateLimiter.reset(rateLimitKey(assignmentId));

        auditService.recordSuccess(AuditAction.UPDATE, "ExamAssignment", assignmentId, callerUserId,
                null, null, "access_code_generated");

        return new AccessCodeResponse(code);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateAccessCodeResponse validateCode(Long assignmentId, String code) {
        ExamAssignment assignment = loadAssignment(assignmentId);

        if (assignment.getAccessCodeHash() == null) {
            return new ValidateAccessCodeResponse(true);
        }

        String key = rateLimitKey(assignmentId);
        if (rateLimiter.isLocked(key, MAX_FAILED_VALIDATIONS, RATE_LIMIT_WINDOW)) {
            auditService.recordDenied(AuditAction.UPDATE, "ExamAssignment", assignmentId, null,
                    "access code rate limit exceeded");
            return new ValidateAccessCodeResponse(false);
        }

        boolean matches = tokenHasher.hash(code).equals(assignment.getAccessCodeHash());
        boolean statusOk = VALIDATABLE_STATUSES.contains(assignment.getStatus());
        boolean windowOk = isWithinWindow(assignment);

        if (!matches || !statusOk || !windowOk) {
            rateLimiter.recordFailure(key, RATE_LIMIT_WINDOW);
            auditService.recordDenied(AuditAction.UPDATE, "ExamAssignment", assignmentId, null,
                    "access code validation failed");
            return new ValidateAccessCodeResponse(false);
        }

        return new ValidateAccessCodeResponse(true);
    }

    private boolean isWithinWindow(ExamAssignment assignment) {
        Instant now = Instant.now();
        if (assignment.getWindowStartAt() != null && now.isBefore(assignment.getWindowStartAt())) {
            return false;
        }
        return assignment.getWindowEndAt() == null || !now.isAfter(assignment.getWindowEndAt());
    }

    private String rateLimitKey(Long assignmentId) {
        return "exam-assignment:" + assignmentId;
    }

    private ExamAssignment loadAssignment(Long assignmentId) {
        return examAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("ExamAssignment not found: " + assignmentId));
    }
}
