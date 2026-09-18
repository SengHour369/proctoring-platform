package com.example.identityservice.auth.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates and hashes the high-entropy random tokens used for refresh tokens, security tokens
 * and API client secrets. These already carry 256 bits of randomness, so a fast digest (SHA-256)
 * is the right tool — unlike passwords, there is nothing for a rainbow table to gain from being
 * slow.
 */
@Component
public class TokenHasher {

    private final SecureRandom secureRandom;

    public TokenHasher(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    /** A URL-safe, at-least-256-bit random token. */
    public String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
