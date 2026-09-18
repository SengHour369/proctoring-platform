package com.example.examservice.exam.dto;

/** The plaintext access code, returned exactly once at generation and never again. */
public record AccessCodeResponse(String code) {
}
