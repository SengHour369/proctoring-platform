package com.example.identityservice.auth.service;

import com.example.identityservice.auth.audit.AuditWriter;
import com.example.identityservice.auth.audit.enums.AuditAction;
import com.example.identityservice.auth.config.AuthProperties;
import com.example.identityservice.auth.dto.LoginResult;
import com.example.identityservice.auth.dto.RequestContext;
import com.example.identityservice.auth.entity.LoginAttempt;
import com.example.identityservice.auth.entity.SecurityToken;
import com.example.identityservice.auth.entity.User;
import com.example.identityservice.auth.entity.UserSession;
import com.example.identityservice.auth.enums.LoginOutcome;
import com.example.identityservice.auth.enums.MfaMethod;
import com.example.identityservice.auth.enums.TokenPurpose;
import com.example.identityservice.auth.enums.UserStatus;
import com.example.identityservice.auth.exception.InvalidSecurityTokenException;
import com.example.identityservice.auth.notification.EmailSender;
import com.example.identityservice.auth.notification.SmsSender;
import com.example.identityservice.auth.repository.LoginAttemptRepository;
import com.example.identityservice.auth.repository.SecurityTokenRepository;
import com.example.identityservice.auth.repository.UserRepository;
import com.example.identityservice.auth.repository.UserSessionRepository;
import com.example.identityservice.auth.security.JwtTokenService;
import com.example.identityservice.auth.security.MfaChallengeStore;
import com.example.identityservice.auth.security.OobCodeStore;
import com.example.identityservice.auth.security.PasswordPolicy;
import com.example.identityservice.auth.security.SecretEncryptor;
import com.example.identityservice.auth.security.TokenHasher;
import com.example.identityservice.auth.security.TotpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Validates human credentials, enforces account state, handles MFA, issues sessions, and manages
 * the one-time verify/reset flows. No response ever reveals whether an email matches an account,
 * and every call writes exactly one {@link LoginAttempt} row.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final SecurityTokenRepository securityTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AuditWriter auditWriter;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;
    private final JwtTokenService jwtTokenService;
    private final MfaChallengeStore mfaChallengeStore;
    private final OobCodeStore oobCodeStore;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final TotpService totpService;
    private final SecretEncryptor secretEncryptor;
    private final PasswordPolicy passwordPolicy;
    private final AuthProperties properties;

    public AuthService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            SecurityTokenRepository securityTokenRepository,
            LoginAttemptRepository loginAttemptRepository,
            AuditWriter auditWriter,
            PasswordEncoder passwordEncoder,
            TokenHasher tokenHasher,
            JwtTokenService jwtTokenService,
            MfaChallengeStore mfaChallengeStore,
            OobCodeStore oobCodeStore,
            EmailSender emailSender,
            SmsSender smsSender,
            TotpService totpService,
            SecretEncryptor secretEncryptor,
            PasswordPolicy passwordPolicy,
            AuthProperties properties) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.securityTokenRepository = securityTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.auditWriter = auditWriter;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.jwtTokenService = jwtTokenService;
        this.mfaChallengeStore = mfaChallengeStore;
        this.oobCodeStore = oobCodeStore;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.totpService = totpService;
        this.secretEncryptor = secretEncryptor;
        this.passwordPolicy = passwordPolicy;
        this.properties = properties;
    }

    @Transactional
    public LoginResult login(String email, String password, RequestContext ctx) {
        String normalizedEmail = email.strip().toLowerCase();

        Optional<User> maybeUser = userRepository.findByEmail(normalizedEmail);
        if (maybeUser.isEmpty()) {
            recordAttempt(null, normalizedEmail, LoginOutcome.BAD_CREDENTIALS, null, ctx);
            return LoginResult.failure(LoginOutcome.BAD_CREDENTIALS);
        }
        User user = maybeUser.get();

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            recordAttempt(user.getId(), normalizedEmail, LoginOutcome.EMAIL_NOT_VERIFIED, null, ctx);
            return LoginResult.failure(LoginOutcome.EMAIL_NOT_VERIFIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            recordAttempt(user.getId(), normalizedEmail, LoginOutcome.ACCOUNT_DISABLED, null, ctx);
            return LoginResult.failure(LoginOutcome.ACCOUNT_DISABLED);
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            recordAttempt(user.getId(), normalizedEmail, LoginOutcome.ACCOUNT_LOCKED, null, ctx);
            return LoginResult.failure(LoginOutcome.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            registerFailedPassword(user);
            recordAttempt(user.getId(), normalizedEmail, LoginOutcome.BAD_CREDENTIALS, "password_mismatch", ctx);
            return LoginResult.failure(LoginOutcome.BAD_CREDENTIALS);
        }

        if (user.isMfaEnabled()) {
            recordAttempt(user.getId(), normalizedEmail, LoginOutcome.MFA_REQUIRED, null, ctx);
            mfaChallengeStore.issue(user.getId(), properties.getMfaChallengeTtl());
            if (user.getMfaMethod() == MfaMethod.EMAIL) {
                String code = oobCodeStore.issue(user.getId(), properties.getMfaChallengeTtl());
                emailSender.sendMfaCode(user.getEmail(), code);
            } else if (user.getMfaMethod() == MfaMethod.SMS) {
                String code = oobCodeStore.issue(user.getId(), properties.getMfaChallengeTtl());
                smsSender.sendMfaCode(user.getPhoneNumber(), code);
            }
            return LoginResult.requireMfa();
        }

        LoginResult result = issueSession(user, ctx);
        recordAttempt(user.getId(), normalizedEmail, LoginOutcome.SUCCESS, null, ctx);
        return result;
    }

    @Transactional
    public LoginResult verifyMfaCode(Long userId, String code, RequestContext ctx) {
        User user = userRepository.findById(userId).orElseThrow();

        if (!user.isMfaEnabled() || user.getMfaMethod() == null || !mfaChallengeStore.hasLiveChallenge(userId)) {
            recordAttempt(userId, user.getEmail(), LoginOutcome.MFA_FAILED, null, ctx);
            return LoginResult.failure(LoginOutcome.MFA_FAILED);
        }

        boolean valid = switch (user.getMfaMethod()) {
            case TOTP -> {
                String seed = secretEncryptor.decrypt(user.getMfaSecretEncrypted());
                yield totpService.verify(seed, code, properties.getTotpWindowSteps());
            }
            case EMAIL, SMS -> oobCodeStore.verify(userId, code);
        };

        if (!valid) {
            registerFailedPassword(user);
            recordAttempt(userId, user.getEmail(), LoginOutcome.MFA_FAILED, null, ctx);
            return LoginResult.failure(LoginOutcome.MFA_FAILED);
        }

        mfaChallengeStore.consume(userId);
        if (user.getMfaMethod() == MfaMethod.EMAIL || user.getMfaMethod() == MfaMethod.SMS) {
            oobCodeStore.consume(userId);
        }
        LoginResult result = issueSession(user, ctx);
        recordAttempt(userId, user.getEmail(), LoginOutcome.SUCCESS, null, ctx);
        return result;
    }

    @Transactional
    public void logout(Long sessionId) {
        UserSession session = userSessionRepository.findById(sessionId).orElseThrow();
        if (session.getRevokedAt() != null) {
            return;
        }
        session.setRevokedAt(Instant.now());
        session.setRevokedReason("user_logout");
        userSessionRepository.save(session);

        auditWriter.write(session.getUserId(), AuditAction.LOGOUT, "UserSession", sessionId, null, null, "user_logout");
    }

    /**
     * When {@code app.auth.refresh-token-rotation-enabled} is off, the returned {@link LoginResult}
     * carries a {@code null} refreshToken — the caller keeps using the one it already has.
     */
    @Transactional
    public LoginResult refreshToken(String refreshToken) {
        String hash = tokenHasher.hash(refreshToken);
        Optional<UserSession> maybeSession = userSessionRepository.findByRefreshTokenHash(hash);
        if (maybeSession.isEmpty()) {
            return LoginResult.failure(LoginOutcome.BAD_CREDENTIALS);
        }
        UserSession session = maybeSession.get();
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(Instant.now())) {
            return LoginResult.failure(LoginOutcome.TOKEN_EXPIRED);
        }
        User user = userRepository.findById(session.getUserId()).orElseThrow();
        if (user.getStatus() != UserStatus.ACTIVE) {
            return LoginResult.failure(LoginOutcome.ACCOUNT_DISABLED);
        }

        String newRefreshToken = null;
        if (properties.isRefreshTokenRotationEnabled()) {
            newRefreshToken = tokenHasher.generateToken();
            session.setRefreshTokenHash(tokenHasher.hash(newRefreshToken));
            session.setExpiresAt(Instant.now().plus(properties.getSessionTtl()));
        }
        session.setLastSeenAt(Instant.now());
        userSessionRepository.save(session);

        String accessToken = jwtTokenService.generateAccessToken(user.getId());
        return LoginResult.success(accessToken, newRefreshToken, session.getId());
    }

    /**
     * Returns the plaintext reset token for the caller to dispatch by email, or {@code null} if
     * the address matches no account — the caller must still show an identical outward response
     * either way, so no response reveals whether the email exists.
     */
    @Transactional
    public String forgotPassword(String email, RequestContext ctx) {
        String normalizedEmail = email.strip().toLowerCase();
        Optional<User> maybeUser = userRepository.findByEmail(normalizedEmail);
        if (maybeUser.isEmpty()) {
            return null;
        }
        User user = maybeUser.get();
        String token = tokenHasher.generateToken();
        SecurityToken securityToken = new SecurityToken();
        securityToken.setUserId(user.getId());
        securityToken.setPurpose(TokenPurpose.PASSWORD_RESET);
        securityToken.setTokenHash(tokenHasher.hash(token));
        securityToken.setIssuedAt(Instant.now());
        securityToken.setExpiresAt(Instant.now().plus(properties.getPasswordResetTokenTtl()));
        securityToken.setRequestedIp(ctx.ipAddress());
        securityTokenRepository.save(securityToken);

        auditWriter.write(null, AuditAction.UPDATE, "SecurityToken", securityToken.getId(), null, null,
                "password_reset_requested");

        return token;
    }

    /** Validates {@code newPassword} against length, complexity, and breach-list rules. */
    @Transactional
    public void resetPassword(String token, String newPassword, String requestIp) {
        passwordPolicy.validate(newPassword);
        SecurityToken securityToken = loadLiveToken(token, TokenPurpose.PASSWORD_RESET);

        User user = userRepository.findById(securityToken.getUserId()).orElseThrow();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        securityToken.setUsedAt(Instant.now());
        securityToken.setRedeemedIp(requestIp);
        securityTokenRepository.save(securityToken);

        List<UserSession> sessions = userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        for (UserSession session : sessions) {
            session.setRevokedAt(Instant.now());
            session.setRevokedReason("password_reset");
        }
        userSessionRepository.saveAll(sessions);

        auditWriter.write(user.getId(), AuditAction.UPDATE, "User", user.getId(), null, null, "password_reset");
    }

    @Transactional
    public void verifyEmail(String token, String requestIp) {
        SecurityToken securityToken = loadLiveToken(token, TokenPurpose.EMAIL_VERIFICATION);

        User user = userRepository.findById(securityToken.getUserId()).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        securityToken.setUsedAt(Instant.now());
        securityToken.setRedeemedIp(requestIp);
        securityTokenRepository.save(securityToken);

        auditWriter.write(user.getId(), AuditAction.UPDATE, "User", user.getId(),
                "{\"status\":\"PENDING_VERIFICATION\"}", "{\"status\":\"ACTIVE\"}", null);
    }

    /**
     * For TOTP, returns the plaintext seed once for QR setup; SMS/EMAIL store no secret and instead
     * reuse {@code phoneNumber}/{@code email} as the delivery destination.
     */
    @Transactional
    public String enrollMfa(Long userId, MfaMethod method) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Only an active user may enroll MFA");
        }

        String plaintextSeed = null;
        if (method == MfaMethod.TOTP) {
            plaintextSeed = totpService.generateSeed(new java.security.SecureRandom());
            user.setMfaSecretEncrypted(secretEncryptor.encrypt(plaintextSeed));
        } else if (method == MfaMethod.SMS) {
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                throw new IllegalStateException("A phone number on file is required to enroll SMS MFA");
            }
        } else if (method == MfaMethod.EMAIL) {
            if (user.getEmailVerifiedAt() == null) {
                throw new IllegalStateException("A verified email address is required to enroll email MFA");
            }
        }

        user.setMfaMethod(method);
        user.setMfaEnabled(true);
        user.setMfaEnrolledAt(Instant.now());
        userRepository.save(user);

        auditWriter.write(userId, AuditAction.UPDATE, "User", userId, null, null, "mfa_enrolled");

        return plaintextSeed;
    }

    private SecurityToken loadLiveToken(String token, TokenPurpose expectedPurpose) {
        String hash = tokenHasher.hash(token);
        SecurityToken securityToken = securityTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidSecurityTokenException("Invalid token"));
        if (securityToken.getPurpose() != expectedPurpose
                || securityToken.getUsedAt() != null
                || securityToken.getInvalidatedAt() != null
                || securityToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidSecurityTokenException("Token is not valid for this operation");
        }
        return securityToken;
    }

    private LoginResult issueSession(User user, RequestContext ctx) {
        user.setFailedLoginCount(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String refreshToken = tokenHasher.generateToken();
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setRefreshTokenHash(tokenHasher.hash(refreshToken));
        session.setIpAddress(ctx.ipAddress());
        session.setUserAgent(ctx.userAgent());
        session.setDeviceFingerprint(ctx.deviceFingerprint());
        session.setIssuedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(properties.getSessionTtl()));
        session.setLastSeenAt(Instant.now());
        userSessionRepository.save(session);

        String accessToken = jwtTokenService.generateAccessToken(user.getId());
        return LoginResult.success(accessToken, refreshToken, session.getId());
    }

    private void registerFailedPassword(User user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        if (user.getFailedLoginCount() >= properties.getLockoutThreshold()) {
            user.setLockedUntil(Instant.now().plus(properties.getLockDuration()));
            user.setFailedLoginCount(0);
        }
        userRepository.save(user);
    }

    private void recordAttempt(Long userId, String email, LoginOutcome outcome, String failureDetail, RequestContext ctx) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(userId);
        attempt.setEmail(email);
        attempt.setOutcome(outcome);
        attempt.setAttemptedAt(Instant.now());
        attempt.setIpAddress(ctx.ipAddress());
        attempt.setUserAgent(ctx.userAgent());
        attempt.setDeviceFingerprint(ctx.deviceFingerprint());
        attempt.setGeoCountry(ctx.geoCountry());
        attempt.setFailureDetail(failureDetail);
        loginAttemptRepository.save(attempt);
    }
}
