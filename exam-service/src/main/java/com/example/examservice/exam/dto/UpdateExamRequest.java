package com.example.examservice.exam.dto;

/**
 * All fields optional — only the ones present are applied. {@code title}/{@code description}/
 * {@code instructions}/{@code showResultImmediately}/{@code holdResultsForReview}/
 * {@code proctoringPolicy}/{@code excelPolicy} are non-structural; {@code maxAttempts}/
 * {@code durationMinutes}/{@code shuffleSections} are structural and, past {@code DRAFT}, bump
 * {@code Exam.version} instead of mutating in place. {@code reason} is required whenever a
 * structural field is set, or when {@code proctoringPolicy} is set on a non-DRAFT exam.
 */
public record UpdateExamRequest(
        String title,
        String description,
        String instructions,
        Boolean showResultImmediately,
        Boolean holdResultsForReview,
        ProctoringPolicyDto proctoringPolicy,
        ExcelPolicyDto excelPolicy,
        Integer maxAttempts,
        Integer durationMinutes,
        Boolean shuffleSections,
        String reason
) {
}
