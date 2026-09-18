package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceQuestionRequest(
        @NotNull Long questionId,
        Integer sequenceNo,
        @NotNull BigDecimal points,
        BigDecimal negativePoints,
        Boolean required,
        Boolean shuffleOptions,
        String reason
) {
}
