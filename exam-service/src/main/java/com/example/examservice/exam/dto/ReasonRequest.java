package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotBlank;

/** Shared body for actions that require a reason: delete, close, archive. */
public record ReasonRequest(@NotBlank String reason) {
}
