package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.ExamInvitationResponse;
import com.example.examservice.exam.dto.SendInvitationRequest;

public interface ExamInvitationService {

    ExamInvitationResponse sendInvitation(Long assignmentId, SendInvitationRequest request, Long callerUserId);

    ExamInvitationResponse resend(Long assignmentId, SendInvitationRequest request, Long callerUserId);

    ExamInvitationResponse recordOpen(Long invitationId, String token);

    ExamInvitationResponse recordAccept(Long invitationId, String token);
}
