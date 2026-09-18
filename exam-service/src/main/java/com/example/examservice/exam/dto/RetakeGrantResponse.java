package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.RetakeGrant;

import java.time.Instant;

public record RetakeGrantResponse(
        Long id,
        Long examAssignmentId,
        Long candidateUserId,
        Long reviewDecisionId,
        Long grantedByUserId,
        Instant grantedAt,
        int additionalAttempts,
        Instant expiresAt,
        Instant consumedAt,
        Long consumedByAttemptId,
        String reason,
        Long supersedesGrantId
) {

    public static RetakeGrantResponse from(RetakeGrant grant) {
        return new RetakeGrantResponse(
                grant.getId(),
                grant.getExamAssignmentId(),
                grant.getCandidateUserId(),
                grant.getReviewDecisionId(),
                grant.getGrantedByUserId(),
                grant.getGrantedAt(),
                grant.getAdditionalAttempts(),
                grant.getExpiresAt(),
                grant.getConsumedAt(),
                grant.getConsumedByAttemptId(),
                grant.getReason(),
                grant.getSupersedesGrantId());
    }
}
