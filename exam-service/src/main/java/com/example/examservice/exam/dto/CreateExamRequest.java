package com.example.examservice.exam.dto;

import com.example.examservice.exam.enums.GradingMode;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateExamRequest(
        @NotBlank String code,
        @NotBlank String title,
        String description,
        String instructions,
        Integer durationMinutes,
        Instant opensAt,
        Instant closesAt,
        Integer maxAttempts,
        BigDecimal totalPoints,
        BigDecimal passingScore,
        GradingMode gradingMode,
        Boolean shuffleSections,
        Boolean holdResultsForReview,
        Boolean showResultImmediately,
        ProctoringPolicyDto proctoringPolicy,
        ExcelPolicyDto excelPolicy
) {
}
