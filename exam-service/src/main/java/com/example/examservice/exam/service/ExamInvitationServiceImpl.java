package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.common.security.TokenHasher;
import com.example.examservice.exam.dto.ExamInvitationResponse;
import com.example.examservice.exam.dto.SendInvitationRequest;
import com.example.examservice.exam.entity.ExamAssignment;
import com.example.examservice.exam.entity.ExamInvitation;
import com.example.examservice.exam.enums.AssignmentStatus;
import com.example.examservice.exam.enums.InvitationStatus;
import com.example.examservice.exam.port.CandidateDirectoryPort;
import com.example.examservice.exam.port.NotificationDispatchPort;
import com.example.examservice.exam.repository.ExamAssignmentRepository;
import com.example.examservice.exam.repository.ExamInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamInvitationServiceImpl implements ExamInvitationService {

    private static final Set<AssignmentStatus> INVITABLE_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.NOTIFIED);

    private static final Duration INVITATION_TTL = Duration.ofDays(7);

    private final ExamInvitationRepository examInvitationRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final CandidateDirectoryPort candidateDirectoryPort;
    private final NotificationDispatchPort notificationDispatchPort;
    private final TokenHasher tokenHasher;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public ExamInvitationResponse sendInvitation(Long assignmentId, SendInvitationRequest request,
                                                  Long callerUserId) {
        return dispatch(assignmentId, request, callerUserId, 1, false);
    }

    @Override
    @Transactional
    public ExamInvitationResponse resend(Long assignmentId, SendInvitationRequest request, Long callerUserId) {
        List<ExamInvitation> existing =
                examInvitationRepository.findByExamAssignmentIdOrderBySequenceNoDesc(assignmentId);
        int nextSequenceNo = existing.isEmpty() ? 1 : existing.get(0).getSequenceNo() + 1;
        return dispatch(assignmentId, request, callerUserId, nextSequenceNo, true);
    }

    private ExamInvitationResponse dispatch(Long assignmentId, SendInvitationRequest request, Long callerUserId,
                                             int sequenceNo, boolean isReminder) {
        requirePermission(assignmentId, callerUserId);

        ExamAssignment assignment = loadAssignment(assignmentId);
        if (!INVITABLE_STATUSES.contains(assignment.getStatus())) {
            throw new InvalidRequestException("assignment is not in an invitable status");
        }

        String sentTo = candidateDirectoryPort
                .resolveContactAddress(assignment.getCandidateUserId(), request.channel())
                .orElseThrow(() -> new InvalidRequestException(
                        "candidate has no address for channel " + request.channel()));

        String plaintextToken = tokenHasher.generateToken();

        ExamInvitation invitation = new ExamInvitation();
        invitation.setExamAssignmentId(assignmentId);
        invitation.setChannel(request.channel());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setSentTo(sentTo);
        invitation.setTokenHash(tokenHasher.hash(plaintextToken));
        invitation.setSequenceNo(sequenceNo);
        invitation.setReminder(isReminder);
        invitation.setExpiresAt(Instant.now().plus(INVITATION_TTL));

        NotificationDispatchPort.DispatchResult result = notificationDispatchPort.dispatchInvitation(
                assignment.getCandidateUserId(), sentTo, request.channel(), plaintextToken);

        if (result.success()) {
            invitation.setStatus(InvitationStatus.SENT);
            invitation.setSentAt(Instant.now());
        } else {
            invitation.setStatus(InvitationStatus.FAILED);
            invitation.setFailureReason(result.failureReason());
        }
        invitation = examInvitationRepository.save(invitation);

        if (result.success() && assignment.getStatus() == AssignmentStatus.ASSIGNED) {
            assignment.setStatus(AssignmentStatus.NOTIFIED);
            assignment.setNotifiedAt(Instant.now());
            examAssignmentRepository.save(assignment);
        }

        auditService.recordSuccess(AuditAction.ASSIGN, "ExamInvitation", invitation.getId(), callerUserId,
                null, null, isReminder ? "invitation_resent" : "invitation_sent");

        return ExamInvitationResponse.from(invitation);
    }

    @Override
    @Transactional
    public ExamInvitationResponse recordOpen(Long invitationId, String token) {
        ExamInvitation invitation = matchToken(invitationId, token);

        if (invitation.getExpiresAt() != null && !invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            examInvitationRepository.save(invitation);
            throw new InvalidRequestException("invitation has expired");
        }
        if (invitation.getStatus() == InvitationStatus.CANCELLED || invitation.getStatus() == InvitationStatus.FAILED) {
            throw new InvalidRequestException("invitation is not open-able in status " + invitation.getStatus());
        }

        if (invitation.getOpenedAt() == null) {
            invitation.setOpenedAt(Instant.now());
            invitation.setStatus(InvitationStatus.OPENED);
            invitation = examInvitationRepository.save(invitation);
            auditService.recordSuccess(AuditAction.UPDATE, "ExamInvitation", invitationId, null,
                    null, null, "invitation_opened");
        }
        return ExamInvitationResponse.from(invitation);
    }

    @Override
    @Transactional
    public ExamInvitationResponse recordAccept(Long invitationId, String token) {
        ExamInvitation invitation = matchToken(invitationId, token);

        if (invitation.getExpiresAt() != null && !invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            examInvitationRepository.save(invitation);
            throw new InvalidRequestException("invitation has expired");
        }

        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            return ExamInvitationResponse.from(invitation);
        }
        if (invitation.getStatus() != InvitationStatus.SENT && invitation.getStatus() != InvitationStatus.OPENED) {
            throw new InvalidRequestException("invitation must be SENT or OPENED to accept, was "
                    + invitation.getStatus());
        }

        invitation.setAcceptedAt(Instant.now());
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation = examInvitationRepository.save(invitation);

        auditService.recordSuccess(AuditAction.UPDATE, "ExamInvitation", invitationId, null,
                null, null, "invitation_accepted");

        return ExamInvitationResponse.from(invitation);
    }

    private ExamInvitation matchToken(Long invitationId, String token) {
        ExamInvitation invitation = examInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("ExamInvitation not found: " + invitationId));
        if (invitation.getTokenHash() == null || !invitation.getTokenHash().equals(tokenHasher.hash(token))) {
            throw new InvalidRequestException("invalid invitation token");
        }
        return invitation;
    }

    private ExamAssignment loadAssignment(Long assignmentId) {
        return examAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("ExamAssignment not found: " + assignmentId));
    }

    private void requirePermission(Long assignmentId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, Permissions.EXAM_INVITE)) {
            auditService.recordDenied(AuditAction.ASSIGN, "ExamInvitation", assignmentId, callerUserId,
                    "missing " + Permissions.EXAM_INVITE);
            throw new AccessDeniedException("missing permission: " + Permissions.EXAM_INVITE);
        }
    }
}
