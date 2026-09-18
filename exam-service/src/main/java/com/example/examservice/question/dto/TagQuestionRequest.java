package com.example.examservice.question.dto;

import jakarta.validation.constraints.NotBlank;

public record TagQuestionRequest(@NotBlank String tagName, String tagDescription) {
}
