package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateSectionRequest(
        @NotBlank String title,
        String description,
        String instructions,
        Integer sequenceNo,
        Integer timeLimitMinutes,
        BigDecimal sectionPoints,
        Boolean shuffleQuestions,
        Integer questionsToDraw,
        Boolean lockOnExit,
        String reason
) {
}
