package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.Exam;
import com.example.examservice.exam.enums.ExamStatus;
import com.example.examservice.exam.enums.GradingMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamResponse(
        Long id,
        UUID publicId,
        String code,
        String title,
        String description,
        String instructions,
        ExamStatus status,
        int version,
        Integer durationMinutes,
        Instant opensAt,
        Instant closesAt,
        int maxAttempts,
        BigDecimal totalPoints,
        BigDecimal passingScore,
        GradingMode gradingMode,
        boolean shuffleSections,
        boolean holdResultsForReview,
        boolean showResultImmediately
) {

    public static ExamResponse from(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getPublicId(),
                exam.getCode(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getInstructions(),
                exam.getStatus(),
                exam.getVersion(),
                exam.getDurationMinutes(),
                exam.getOpensAt(),
                exam.getClosesAt(),
                exam.getMaxAttempts(),
                exam.getTotalPoints(),
                exam.getPassingScore(),
                exam.getGradingMode(),
                exam.isShuffleSections(),
                exam.isHoldResultsForReview(),
                exam.isShowResultImmediately());
    }
}
