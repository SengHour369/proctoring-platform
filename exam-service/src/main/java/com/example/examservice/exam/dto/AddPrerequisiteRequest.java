package com.example.examservice.exam.dto;

import java.math.BigDecimal;

public record AddPrerequisiteRequest(
        Long requiredExamId,
        BigDecimal minScore,
        String courseReference,
        String description
) {
}
