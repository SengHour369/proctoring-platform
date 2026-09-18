package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.ExamWindowOverride;

import java.time.Instant;

public record ExamWindowOverrideResponse(
        Long id,
        Long examAssignmentId,
        Instant overriddenWindowStartAt,
        Instant overriddenWindowEndAt,
        String reason,
        String justificationRef,
        Long grantedByUserId,
        Instant grantedAt,
        Instant expiresAt,
        Long supersedesOverrideId
) {

    public static ExamWindowOverrideResponse from(ExamWindowOverride override) {
        return new ExamWindowOverrideResponse(
                override.getId(),
                override.getExamAssignmentId(),
                override.getOverriddenWindowStartAt(),
                override.getOverriddenWindowEndAt(),
                override.getReason(),
                override.getJustificationRef(),
                override.getGrantedByUserId(),
                override.getGrantedAt(),
                override.getExpiresAt(),
                override.getSupersedesOverrideId());
    }
}
