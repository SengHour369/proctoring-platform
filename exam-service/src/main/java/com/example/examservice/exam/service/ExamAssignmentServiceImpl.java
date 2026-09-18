package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.exam.dto.AssignToGroupRequest;
import com.example.examservice.exam.dto.AssignToStudentRequest;
import com.example.examservice.exam.dto.EligibilityResponse;
import com.example.examservice.exam.dto.ExamAssignmentResponse;
import com.example.examservice.exam.dto.GroupAssignmentResponse;
import com.example.examservice.exam.entity.Exam;
import com.example.examservice.exam.entity.ExamAssignment;
import com.example.examservice.exam.entity.ExamGroupAssignment;
import com.example.examservice.exam.entity.ExamInvitation;
import com.example.examservice.exam.enums.AssignmentStatus;
import com.example.examservice.exam.enums.ExamStatus;
import com.example.examservice.exam.enums.InvitationStatus;
import com.example.examservice.exam.port.CandidateDirectoryPort;
import com.example.examservice.exam.port.GroupMembershipPort;
import com.example.examservice.exam.repository.ExamAssignmentRepository;
import com.example.examservice.exam.repository.ExamGroupAssignmentRepository;
import com.example.examservice.exam.repository.ExamInvitationRepository;
import com.example.examservice.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamAssignmentServiceImpl implements ExamAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(ExamAssignmentServiceImpl.class);

    private static final Set<ExamStatus> ASSIGNABLE_EXAM_STATUSES =
            EnumSet.of(ExamStatus.SCHEDULED, ExamStatus.PUBLISHED, ExamStatus.ACTIVE);

    private static final Set<InvitationStatus> INVITATION_TERMINAL_STATUSES =
            EnumSet.of(InvitationStatus.EXPIRED, InvitationStatus.CANCELLED, InvitationStatus.FAILED);

    private final ExamAssignmentRepository examAssignmentRepository;
    private final ExamGroupAssignmentRepository examGroupAssignmentRepository;
    private final ExamInvitationRepository examInvitationRepository;
    private final ExamRepository examRepository;
    private final ExamPrerequisiteService examPrerequisiteService;
    private final ExamWindowOverrideService examWindowOverrideService;
    private final CandidateDirectoryPort candidateDirectoryPort;
    private final GroupMembershipPort groupMembershipPort;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public ExamAssignmentResponse assignToStudent(Long examId, AssignToStudentRequest request, Long callerUserId) {
        requirePermission(examId, callerUserId);

        Exam exam = loadExam(examId);
        if (!ASSIGNABLE_EXAM_STATUSES.contains(exam.getStatus())) {
            throw new InvalidRequestException("exam is not in an assignable status");
        }
        if (!candidateDirectoryPort.isActiveStudent(request.candidateUserId())) {
            throw new InvalidRequestException("candidate is not an active student");
        }

        EligibilityResponse eligibility = examPrerequisiteService.checkEligibility(examId, request.candidateUserId());
        if (!eligibility.eligible()) {
            auditService.recordDenied(AuditAction.ASSIGN, "ExamAssignment", null, callerUserId,
                    "prerequisite_failed");
            throw new InvalidRequestException("candidate does not meet the exam's prerequisites");
        }

        Optional<ExamAssignment> existing =
                examAssignmentRepository.findByExamIdAndCandidateUserId(examId, request.candidateUserId());

        Instant windowStartAt = request.windowStartAt() != null ? request.windowStartAt() : exam.getOpensAt();
        Instant windowEndAt = request.windowEndAt() != null ? request.windowEndAt() : exam.getClosesAt();
        validateWindow(exam, existing.map(ExamAssignment::getId).orElse(null), windowStartAt, windowEndAt);

        if (request.dueAt() != null && windowEndAt != null && request.dueAt().isAfter(windowEndAt)) {
            throw new InvalidRequestException("dueAt must not be after windowEndAt");
        }

        if (request.attemptsAllowed() != null) {
            if (request.attemptsAllowed() < 1) {
                throw new InvalidRequestException("attemptsAllowed must be >= 1");
            }
            if (request.attemptsAllowed() > exam.getMaxAttempts()
                    && !roleService.checkAccess(callerUserId, Permissions.EXAM_GRANT_EXTRA_ATTEMPTS)) {
                throw new InvalidRequestException(
                        "attemptsAllowed exceeds the exam's maxAttempts and caller lacks "
                                + Permissions.EXAM_GRANT_EXTRA_ATTEMPTS);
            }
        }

        ExamAssignment assignment;
        if (existing.isPresent()) {
            assignment = existing.get();
            if (assignment.getStatus() == AssignmentStatus.SUBMITTED) {
                throw new InvalidRequestException("assignment is already SUBMITTED and cannot be edited");
            }
            if (assignment.getStatus() == AssignmentStatus.CANCELLED) {
                throw new InvalidRequestException(
                        "assignment is CANCELLED; reactivation is a separate operation");
            }
            Map<String, Object> before = Map.of(
                    "windowStartAt", String.valueOf(assignment.getWindowStartAt()),
                    "windowEndAt", String.valueOf(assignment.getWindowEndAt()));
            assignment.setWindowStartAt(windowStartAt);
            assignment.setWindowEndAt(windowEndAt);
            assignment.setDueAt(request.dueAt());
            assignment.setAttemptsAllowed(request.attemptsAllowed());
            assignment.setExtraTimeMinutes(request.extraTimeMinutes());
            assignment = examAssignmentRepository.save(assignment);

            auditService.recordSuccess(AuditAction.UPDATE, "ExamAssignment", assignment.getId(), callerUserId,
                    before, Map.of("windowStartAt", String.valueOf(windowStartAt),
                            "windowEndAt", String.valueOf(windowEndAt)), null);
        } else {
            assignment = new ExamAssignment();
            assignment.setExamId(examId);
            assignment.setCandidateUserId(request.candidateUserId());
            assignment.setAssignedByUserId(callerUserId);
            assignment.setStatus(AssignmentStatus.ASSIGNED);
            assignment.setAssignedAt(Instant.now());
            assignment.setWindowStartAt(windowStartAt);
            assignment.setWindowEndAt(windowEndAt);
            assignment.setDueAt(request.dueAt());
            assignment.setAttemptsAllowed(request.attemptsAllowed());
            assignment.setExtraTimeMinutes(request.extraTimeMinutes());
            assignment = examAssignmentRepository.save(assignment);

            auditService.recordSuccess(AuditAction.ASSIGN, "ExamAssignment", assignment.getId(), callerUserId,
                    null, Map.of("examId", examId, "candidateUserId", request.candidateUserId(),
                            "windowStartAt", String.valueOf(windowStartAt),
                            "windowEndAt", String.valueOf(windowEndAt)), null);
        }

        return ExamAssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public GroupAssignmentResponse assignToGroup(Long examId, AssignToGroupRequest request, Long callerUserId) {
        return assignToGroupInternal(examId, request, callerUserId, false);
    }

    @Override
    @Transactional
    public GroupAssignmentResponse assignToClass(Long examId, AssignToGroupRequest request, Long callerUserId) {
        return assignToGroupInternal(examId, request, callerUserId, true);
    }

    private GroupAssignmentResponse assignToGroupInternal(Long examId, AssignToGroupRequest request,
                                                            Long callerUserId, boolean requireClass) {
        requirePermission(examId, callerUserId);

        Exam exam = loadExam(examId);
        if (!ASSIGNABLE_EXAM_STATUSES.contains(exam.getStatus())) {
            throw new InvalidRequestException("exam is not in an assignable status");
        }

        GroupMembershipPort.GroupSummary group = groupMembershipPort.findGroup(request.studentGroupId())
                .orElseThrow(() -> new EntityNotFoundException("StudentGroup not found: " + request.studentGroupId()));
        if (!group.active()) {
            throw new InvalidRequestException("student group is not active");
        }
        if (requireClass && !"CLASS".equals(group.groupType())) {
            throw new InvalidRequestException("studentGroupId does not refer to a CLASS group");
        }

        Instant windowStartAt = request.windowStartAt() != null ? request.windowStartAt() : exam.getOpensAt();
        Instant windowEndAt = request.windowEndAt() != null ? request.windowEndAt() : exam.getClosesAt();
        validateWindow(exam, null, windowStartAt, windowEndAt);

        Optional<ExamGroupAssignment> existing =
                examGroupAssignmentRepository.findByExamIdAndStudentGroupId(examId, request.studentGroupId());

        ExamGroupAssignment groupAssignment;
        if (existing.isPresent()) {
            groupAssignment = existing.get();
            if (groupAssignment.getCancelledAt() != null) {
                throw new InvalidRequestException("group assignment was cancelled");
            }
            groupAssignment.setWindowStartAt(windowStartAt);
            groupAssignment.setWindowEndAt(windowEndAt);
            groupAssignment.setDueAt(request.dueAt());
            groupAssignment.setAutoEnrollNewMembers(
                    request.autoEnrollNewMembers() != null && request.autoEnrollNewMembers());
        } else {
            groupAssignment = new ExamGroupAssignment();
            groupAssignment.setExamId(examId);
            groupAssignment.setStudentGroupId(request.studentGroupId());
            groupAssignment.setAssignedByUserId(callerUserId);
            groupAssignment.setAssignedAt(Instant.now());
            groupAssignment.setWindowStartAt(windowStartAt);
            groupAssignment.setWindowEndAt(windowEndAt);
            groupAssignment.setDueAt(request.dueAt());
            groupAssignment.setAutoEnrollNewMembers(
                    request.autoEnrollNewMembers() != null && request.autoEnrollNewMembers());
            groupAssignment.setExpandedCount(0);
        }
        groupAssignment = examGroupAssignmentRepository.save(groupAssignment);

        List<Long> memberIds = groupMembershipPort.findActiveMemberUserIds(request.studentGroupId());
        List<GroupAssignmentResponse.SkippedCandidate> skipped = new ArrayList<>();
        int successCount = 0;

        AssignToStudentRequest memberOverrides = new AssignToStudentRequest(
                null, windowStartAt, windowEndAt, request.dueAt(),
                request.attemptsAllowed(), request.extraTimeMinutes());

        for (Long memberId : memberIds) {
            try {
                assignToStudent(examId, withCandidate(memberOverrides, memberId), callerUserId);
                successCount++;
            } catch (InvalidRequestException | AccessDeniedException e) {
                skipped.add(new GroupAssignmentResponse.SkippedCandidate(memberId, e.getMessage()));
            }
        }

        groupAssignment.setExpandedCount(groupAssignment.getExpandedCount() + successCount);
        groupAssignment.setExpandedAt(Instant.now());
        groupAssignment = examGroupAssignmentRepository.save(groupAssignment);

        auditService.recordSuccess(AuditAction.ASSIGN, "ExamGroupAssignment", groupAssignment.getId(), callerUserId,
                null, Map.of("examId", examId, "studentGroupId", request.studentGroupId(),
                        "expandedCount", successCount, "skippedCount", skipped.size()), null);

        return GroupAssignmentResponse.of(groupAssignment, skipped);
    }

    private AssignToStudentRequest withCandidate(AssignToStudentRequest template, Long candidateUserId) {
        return new AssignToStudentRequest(candidateUserId, template.windowStartAt(), template.windowEndAt(),
                template.dueAt(), template.attemptsAllowed(), template.extraTimeMinutes());
    }

    @Override
    @Transactional
    public ExamAssignmentResponse cancelAssignment(Long assignmentId, String reason, Long callerUserId) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("reason is required");
        }
        ExamAssignment assignment = loadAssignment(assignmentId);
        requirePermission(assignment.getExamId(), callerUserId);

        if (assignment.getStatus() == AssignmentStatus.SUBMITTED) {
            throw new InvalidRequestException("a SUBMITTED assignment cannot be cancelled");
        }
        if (assignment.getStatus() == AssignmentStatus.CANCELLED) {
            return ExamAssignmentResponse.from(assignment);
        }

        AssignmentStatus before = assignment.getStatus();
        assignment.setStatus(AssignmentStatus.CANCELLED);
        assignment.setCancelledAt(Instant.now());
        assignment.setCancelReason(reason);
        assignment = examAssignmentRepository.save(assignment);

        List<ExamInvitation> invitations =
                examInvitationRepository.findByExamAssignmentIdOrderBySequenceNoDesc(assignmentId);
        for (ExamInvitation invitation : invitations) {
            if (!INVITATION_TERMINAL_STATUSES.contains(invitation.getStatus())) {
                invitation.setStatus(InvitationStatus.CANCELLED);
                examInvitationRepository.save(invitation);
            }
        }

        auditService.recordSuccess(AuditAction.CANCEL, "ExamAssignment", assignmentId, callerUserId,
                Map.of("status", before), Map.of("status", AssignmentStatus.CANCELLED), reason);

        return ExamAssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public void autoEnrollNewMember(Long studentGroupId, Long candidateUserId) {
        List<ExamGroupAssignment> liveGroupAssignments = examGroupAssignmentRepository
                .findByStudentGroupIdAndCancelledAtIsNullAndAutoEnrollNewMembersTrue(studentGroupId);

        for (ExamGroupAssignment groupAssignment : liveGroupAssignments) {
            AssignToStudentRequest request = new AssignToStudentRequest(
                    candidateUserId, groupAssignment.getWindowStartAt(), groupAssignment.getWindowEndAt(),
                    groupAssignment.getDueAt(), null, null);
            try {
                assignToStudent(groupAssignment.getExamId(), request, groupAssignment.getAssignedByUserId());
                groupAssignment.setExpandedCount(groupAssignment.getExpandedCount() + 1);
                examGroupAssignmentRepository.save(groupAssignment);
                auditService.recordSuccess(AuditAction.ASSIGN, "GroupMembership", null, candidateUserId,
                        null, Map.of("studentGroupId", studentGroupId, "examId", groupAssignment.getExamId()),
                        "auto_enrolled");
            } catch (InvalidRequestException | AccessDeniedException e) {
                log.info("auto-enroll skipped candidate {} for exam {}: {}",
                        candidateUserId, groupAssignment.getExamId(), e.getMessage());
            }
        }
    }

    private void validateWindow(Exam exam, Long examAssignmentId, Instant windowStartAt, Instant windowEndAt) {
        if (windowStartAt != null && windowEndAt != null && !windowStartAt.isBefore(windowEndAt)) {
            throw new InvalidRequestException("windowStartAt must be before windowEndAt");
        }
        if (exam.getOpensAt() != null && windowStartAt != null && windowStartAt.isBefore(exam.getOpensAt())) {
            if (examAssignmentId == null || examWindowOverrideService
                    .findActiveCoveringOverride(examAssignmentId, windowStartAt, windowEndAt).isEmpty()) {
                throw new InvalidRequestException("windowStartAt is before the exam's opensAt and no active "
                        + "window override covers it");
            }
        }
        if (exam.getClosesAt() != null && windowEndAt != null && windowEndAt.isAfter(exam.getClosesAt())) {
            if (examAssignmentId == null || examWindowOverrideService
                    .findActiveCoveringOverride(examAssignmentId, windowStartAt, windowEndAt).isEmpty()) {
                throw new InvalidRequestException("windowEndAt is after the exam's closesAt and no active "
                        + "window override covers it");
            }
        }
    }

    private Exam loadExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
    }

    private ExamAssignment loadAssignment(Long assignmentId) {
        return examAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("ExamAssignment not found: " + assignmentId));
    }

    private void requirePermission(Long examId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, Permissions.EXAM_ASSIGN)) {
            auditService.recordDenied(AuditAction.ASSIGN, "ExamAssignment", examId, callerUserId,
                    "missing " + Permissions.EXAM_ASSIGN);
            throw new AccessDeniedException("missing permission: " + Permissions.EXAM_ASSIGN);
        }
    }
}
