package com.example.identityservice.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Tunable thresholds and TTLs for the authentication/authorization flows. */
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /** Consecutive failed logins before the account is locked. */
    private int lockoutThreshold = 5;

    private Duration lockDuration = Duration.ofMinutes(15);

    private Duration sessionTtl = Duration.ofDays(30);

    /** Whether {@code refreshToken} issues a new refresh token/hash on every use, or only refreshes the access token. */
    private boolean refreshTokenRotationEnabled = true;

    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Duration emailVerificationTokenTtl = Duration.ofHours(24);

    private Duration passwordResetTokenTtl = Duration.ofHours(1);

    private Duration accountInvitationTokenTtl = Duration.ofDays(7);

    private Duration emailChangeTokenTtl = Duration.ofHours(24);

    /** Longest [from, to) window a single activity-view query may span. */
    private int maxActivityRangeDays = 90;

    private Duration mfaChallengeTtl = Duration.ofMinutes(5);

    /** TOTP step tolerance, in 30-second steps either side of the current one. */
    private int totpWindowSteps = 1;

    /** Base64-encoded AES key used to encrypt TOTP seeds at rest. */
    private String mfaSecretEncryptionKey;

    public int getLockoutThreshold() {
        return lockoutThreshold;
    }

    public void setLockoutThreshold(int lockoutThreshold) {
        this.lockoutThreshold = lockoutThreshold;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public boolean isRefreshTokenRotationEnabled() {
        return refreshTokenRotationEnabled;
    }

    public void setRefreshTokenRotationEnabled(boolean refreshTokenRotationEnabled) {
        this.refreshTokenRotationEnabled = refreshTokenRotationEnabled;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getEmailVerificationTokenTtl() {
        return emailVerificationTokenTtl;
    }

    public void setEmailVerificationTokenTtl(Duration emailVerificationTokenTtl) {
        this.emailVerificationTokenTtl = emailVerificationTokenTtl;
    }

    public Duration getPasswordResetTokenTtl() {
        return passwordResetTokenTtl;
    }

    public void setPasswordResetTokenTtl(Duration passwordResetTokenTtl) {
        this.passwordResetTokenTtl = passwordResetTokenTtl;
    }

    public Duration getAccountInvitationTokenTtl() {
        return accountInvitationTokenTtl;
    }

    public void setAccountInvitationTokenTtl(Duration accountInvitationTokenTtl) {
        this.accountInvitationTokenTtl = accountInvitationTokenTtl;
    }

    public Duration getEmailChangeTokenTtl() {
        return emailChangeTokenTtl;
    }

    public void setEmailChangeTokenTtl(Duration emailChangeTokenTtl) {
        this.emailChangeTokenTtl = emailChangeTokenTtl;
    }

    public int getMaxActivityRangeDays() {
        return maxActivityRangeDays;
    }

    public void setMaxActivityRangeDays(int maxActivityRangeDays) {
        this.maxActivityRangeDays = maxActivityRangeDays;
    }

    public Duration getMfaChallengeTtl() {
        return mfaChallengeTtl;
    }

    public void setMfaChallengeTtl(Duration mfaChallengeTtl) {
        this.mfaChallengeTtl = mfaChallengeTtl;
    }

    public int getTotpWindowSteps() {
        return totpWindowSteps;
    }

    public void setTotpWindowSteps(int totpWindowSteps) {
        this.totpWindowSteps = totpWindowSteps;
    }

    public String getMfaSecretEncryptionKey() {
        return mfaSecretEncryptionKey;
    }

    public void setMfaSecretEncryptionKey(String mfaSecretEncryptionKey) {
        this.mfaSecretEncryptionKey = mfaSecretEncryptionKey;
    }
}
