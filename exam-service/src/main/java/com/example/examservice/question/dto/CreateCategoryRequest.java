package com.example.examservice.question.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        Long parentCategoryId
) {
}
