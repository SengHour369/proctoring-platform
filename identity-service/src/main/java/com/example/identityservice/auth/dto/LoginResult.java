package com.example.identityservice.auth.dto;

import com.example.identityservice.auth.enums.LoginOutcome;

/**
 * Result of a login step. {@code outcome} is for internal use (logging, admin tooling) — callers
 * facing the outside world must show an identical message for every non-success, non-MFA outcome
 * so no response reveals whether an email matches an account.
 */
public record LoginResult(
        LoginOutcome outcome,
        boolean mfaRequired,
        String accessToken,
        String refreshToken,
        Long sessionId) {

    public static LoginResult failure(LoginOutcome outcome) {
        return new LoginResult(outcome, false, null, null, null);
    }

    public static LoginResult requireMfa() {
        return new LoginResult(LoginOutcome.MFA_REQUIRED, true, null, null, null);
    }

    public static LoginResult success(String accessToken, String refreshToken, Long sessionId) {
        return new LoginResult(LoginOutcome.SUCCESS, false, accessToken, refreshToken, sessionId);
    }
}
