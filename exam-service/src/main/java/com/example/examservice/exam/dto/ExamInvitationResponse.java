package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.ExamInvitation;
import com.example.examservice.exam.enums.InvitationChannel;
import com.example.examservice.exam.enums.InvitationStatus;

import java.time.Instant;

public record ExamInvitationResponse(
        Long id,
        Long examAssignmentId,
        InvitationChannel channel,
        InvitationStatus status,
        String sentTo,
        int sequenceNo,
        boolean reminder,
        Instant sentAt,
        Instant deliveredAt,
        Instant openedAt,
        Instant acceptedAt,
        Instant expiresAt,
        String failureReason
) {

    public static ExamInvitationResponse from(ExamInvitation invitation) {
        return new ExamInvitationResponse(
                invitation.getId(),
                invitation.getExamAssignmentId(),
                invitation.getChannel(),
                invitation.getStatus(),
                invitation.getSentTo(),
                invitation.getSequenceNo(),
                invitation.isReminder(),
                invitation.getSentAt(),
                invitation.getDeliveredAt(),
                invitation.getOpenedAt(),
                invitation.getAcceptedAt(),
                invitation.getExpiresAt(),
                invitation.getFailureReason());
    }
}
