package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotBlank;

public record InvitationTokenRequest(@NotBlank String token) {
}
