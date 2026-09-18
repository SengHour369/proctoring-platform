package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.ExamGroupAssignment;

import java.util.List;

public record GroupAssignmentResponse(
        Long groupAssignmentId,
        int expandedCount,
        int skippedCount,
        List<SkippedCandidate> skipped
) {

    public static GroupAssignmentResponse of(ExamGroupAssignment groupAssignment, List<SkippedCandidate> skipped) {
        return new GroupAssignmentResponse(
                groupAssignment.getId(),
                groupAssignment.getExpandedCount(),
                skipped.size(),
                skipped);
    }

    public record SkippedCandidate(Long candidateUserId, String reason) {
    }
}
