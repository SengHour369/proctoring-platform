package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AssignToStudentRequest(
        @NotNull Long candidateUserId,
        Instant windowStartAt,
        Instant windowEndAt,
        Instant dueAt,
        Integer attemptsAllowed,
        Integer extraTimeMinutes
) {
}
