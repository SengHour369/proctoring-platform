package com.example.examservice.question.dto;

import com.example.examservice.question.entity.QuestionCategory;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        Long parentCategoryId,
        String path,
        int depth,
        boolean active
) {

    public static CategoryResponse from(QuestionCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getParentCategoryId(),
                category.getPath(),
                category.getDepth(),
                category.isActive());
    }
}
