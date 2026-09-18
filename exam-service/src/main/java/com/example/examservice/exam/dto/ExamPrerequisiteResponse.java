package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.ExamPrerequisite;

import java.math.BigDecimal;

public record ExamPrerequisiteResponse(
        Long id,
        Long examId,
        Long requiredExamId,
        BigDecimal minScore,
        String courseReference,
        String description,
        boolean active
) {

    public static ExamPrerequisiteResponse from(ExamPrerequisite rule) {
        return new ExamPrerequisiteResponse(
                rule.getId(),
                rule.getExamId(),
                rule.getRequiredExamId(),
                rule.getMinScore(),
                rule.getCourseReference(),
                rule.getDescription(),
                rule.isActive());
    }
}
