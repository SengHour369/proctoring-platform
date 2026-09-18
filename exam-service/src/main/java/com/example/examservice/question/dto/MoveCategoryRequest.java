package com.example.examservice.question.dto;

/** {@code newParentCategoryId} null moves the category to the root. */
public record MoveCategoryRequest(Long newParentCategoryId) {
}
