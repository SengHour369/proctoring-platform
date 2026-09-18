package com.example.identityservice.web.dto;

import com.example.identityservice.auth.dto.LoginResult;

/** {@code message} is the only thing shown for a non-success, non-MFA outcome — never the raw {@code outcome}. */
public record LoginResponse(
        boolean success,
        boolean mfaRequired,
        String message,
        String accessToken,
        String refreshToken,
        Long sessionId) {

    public static LoginResponse from(LoginResult result) {
        if (result.mfaRequired()) {
            return new LoginResponse(false, true, "MFA code required", null, null, null);
        }
        return switch (result.outcome()) {
            case SUCCESS -> new LoginResponse(true, false, "Login successful",
                    result.accessToken(), result.refreshToken(), result.sessionId());
            case ACCOUNT_LOCKED -> new LoginResponse(false, false, "Account is temporarily locked", null, null, null);
            case ACCOUNT_DISABLED -> new LoginResponse(false, false, "Account is disabled", null, null, null);
            case EMAIL_NOT_VERIFIED -> new LoginResponse(false, false, "Email is not verified", null, null, null);
            case MFA_FAILED -> new LoginResponse(false, false, "Invalid or expired MFA code", null, null, null);
            case TOKEN_EXPIRED -> new LoginResponse(false, false, "Session has expired, please log in again", null, null, null);
            default -> new LoginResponse(false, false, "Invalid email or password", null, null, null);
        };
    }
}
