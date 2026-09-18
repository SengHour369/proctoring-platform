package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AssignToGroupRequest(
        @NotNull Long studentGroupId,
        Instant windowStartAt,
        Instant windowEndAt,
        Instant dueAt,
        Boolean autoEnrollNewMembers,
        Integer attemptsAllowed,
        Integer extraTimeMinutes
) {
}
