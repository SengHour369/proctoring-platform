package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.ExamAssignment;
import com.example.examservice.exam.enums.AssignmentStatus;

import java.time.Instant;

public record ExamAssignmentResponse(
        Long id,
        Long examId,
        Long candidateUserId,
        Long assignedByUserId,
        AssignmentStatus status,
        Instant assignedAt,
        Instant notifiedAt,
        Instant windowStartAt,
        Instant windowEndAt,
        Instant dueAt,
        Integer attemptsAllowed,
        Integer extraTimeMinutes,
        boolean accessCodeSet,
        Instant cancelledAt,
        String cancelReason
) {

    public static ExamAssignmentResponse from(ExamAssignment assignment) {
        return new ExamAssignmentResponse(
                assignment.getId(),
                assignment.getExamId(),
                assignment.getCandidateUserId(),
                assignment.getAssignedByUserId(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getNotifiedAt(),
                assignment.getWindowStartAt(),
                assignment.getWindowEndAt(),
                assignment.getDueAt(),
                assignment.getAttemptsAllowed(),
                assignment.getExtraTimeMinutes(),
                assignment.getAccessCodeHash() != null,
                assignment.getCancelledAt(),
                assignment.getCancelReason());
    }
}
