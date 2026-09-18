package com.example.examservice.common.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates and hashes the high-entropy random tokens used for invitation links, and the
 * shorter human-typeable codes used for exam access codes. These already carry enough randomness
 * that a fast digest (SHA-256) is the right tool — there is nothing for a rainbow table to gain
 * from being slow, unlike a user password. Mirrors identity-service's {@code TokenHasher}.
 */
@Component
public class TokenHasher {

    private static final String ACCESS_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final SecureRandom secureRandom;

    public TokenHasher(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    /** A URL-safe, at-least-256-bit random token, for invitation links. */
    public String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * A short, human-typeable access code — alphanumeric, uppercase, excluding characters that
     * are easily confused when read aloud or transcribed (no 0/O, 1/I/L).
     */
    public String generateAccessCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ACCESS_CODE_ALPHABET.charAt(secureRandom.nextInt(ACCESS_CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
