package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.ExamWindowOverrideResponse;
import com.example.examservice.exam.dto.GrantWindowOverrideRequest;
import com.example.examservice.exam.entity.ExamWindowOverride;

import java.time.Instant;
import java.util.Optional;

public interface ExamWindowOverrideService {

    ExamWindowOverrideResponse grantOverride(Long assignmentId, GrantWindowOverrideRequest request,
                                              Long callerUserId);

    ExamWindowOverrideResponse revoke(Long overrideId, String reason, Long callerUserId);

    /**
     * The active, unexpired, non-superseded override for the assignment that covers
     * {@code windowStart}/{@code windowEnd}, if one exists. Used by window validation elsewhere —
     * an override never bypasses eligibility, it only widens the sitting window.
     */
    Optional<ExamWindowOverride> findActiveCoveringOverride(Long examAssignmentId, Instant windowStart,
                                                             Instant windowEnd);
}
