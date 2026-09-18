package com.example.identityservice.auth.dto;

/** The plaintext client secret, returned exactly once at creation or rotation. */
public record ApiClientCredential(String clientId, String secret) {
}
