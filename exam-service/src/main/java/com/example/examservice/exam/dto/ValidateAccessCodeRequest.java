package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateAccessCodeRequest(@NotBlank String code) {
}
