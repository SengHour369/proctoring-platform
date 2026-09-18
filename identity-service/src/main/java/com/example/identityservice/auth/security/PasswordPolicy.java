package com.example.identityservice.auth.security;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Enforces the platform password policy: minimum length, character-class complexity, and
 * rejection of passwords from a small known-breached/common-password list. Not a substitute for a
 * live breach-corpus lookup (e.g. HaveIBeenPwned k-anonymity API) — that is a reasonable future
 * upgrade — but it stops the most common weak passwords today.
 */
@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 12;

    /** A small sample of the most commonly breached/guessed passwords, lower-cased. */
    private static final Set<String> COMMON_BREACHED_PASSWORDS = Set.of(
            "password", "password1", "password123", "123456", "12345678", "123456789",
            "qwerty123", "qwertyuiop", "letmein123", "iloveyou1", "admin1234", "welcome123",
            "changeme1", "sunshine1", "princess1", "football1", "baseball1", "dragon123",
            "monkey123", "trustno1", "abc123456", "1qaz2wsx3", "superman1", "master1234");

    /**
     * Validates {@code password} against length, complexity, and breach-list rules.
     *
     * @throws IllegalArgumentException with a human-readable reason on the first rule violated
     */
    public void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException(
                    "Password must include an uppercase letter, a lowercase letter, a digit, and a special character");
        }
        if (COMMON_BREACHED_PASSWORDS.contains(password.toLowerCase())) {
            throw new IllegalArgumentException("Password is too common and appears in known breach lists");
        }
    }
}
