package com.example.identityservice.auth.service;

import com.example.identityservice.auth.audit.entity.AuditLog;
import com.example.identityservice.auth.audit.enums.AuditAction;
import com.example.identityservice.auth.audit.repository.AuditLogRepository;
import com.example.identityservice.auth.dto.ApiClientCredential;
import com.example.identityservice.auth.entity.ApiClient;
import com.example.identityservice.auth.enums.ApiClientStatus;
import com.example.identityservice.auth.repository.ApiClientRepository;
import com.example.identityservice.auth.security.CidrMatcher;
import com.example.identityservice.auth.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Credentials and authentication for machine callers — never routed through {@code UserRole}. */
@Service
public class ApiClientService {

    private final ApiClientRepository apiClientRepository;
    private final AuditLogRepository auditLogRepository;
    private final TokenHasher tokenHasher;

    /** Per-client sliding request timestamps for {@code rateLimitPerMinute} enforcement. */
    private final ConcurrentHashMap<String, java.util.Deque<Instant>> requestWindows = new ConcurrentHashMap<>();

    public ApiClientService(ApiClientRepository apiClientRepository, AuditLogRepository auditLogRepository, TokenHasher tokenHasher) {
        this.apiClientRepository = apiClientRepository;
        this.auditLogRepository = auditLogRepository;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public ApiClientCredential registerClient(
            String name, String allowedScopes, Long createdByUserId, Integer rateLimitPerMinute, String allowedIpRanges) {
        String clientId = UUID.randomUUID().toString();
        String secret = tokenHasher.generateToken();

        ApiClient apiClient = new ApiClient();
        apiClient.setClientId(clientId);
        apiClient.setName(name);
        apiClient.setClientSecretHash(tokenHasher.hash(secret));
        apiClient.setAllowedScopes(allowedScopes);
        apiClient.setStatus(ApiClientStatus.ACTIVE);
        apiClient.setRateLimitPerMinute(rateLimitPerMinute);
        apiClient.setAllowedIpRanges(allowedIpRanges);
        apiClient.setCreatedByUserId(createdByUserId);
        apiClientRepository.save(apiClient);

        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(createdByUserId);
        auditLog.setAction(AuditAction.CREATE);
        auditLog.setEntityType("ApiClient");
        auditLog.setEntityId(apiClient.getId());
        auditLogRepository.save(auditLog);

        return new ApiClientCredential(clientId, secret);
    }

    @Transactional
    public ApiClientCredential rotateSecret(String clientId) {
        ApiClient apiClient = apiClientRepository.findByClientId(clientId).orElseThrow();
        if (apiClient.getStatus() != ApiClientStatus.ACTIVE) {
            throw new IllegalStateException("Client is not active");
        }

        String secret = tokenHasher.generateToken();
        apiClient.setClientSecretHash(tokenHasher.hash(secret));
        apiClient.setSecretRotatedAt(Instant.now());
        apiClientRepository.save(apiClient);

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(AuditAction.UPDATE);
        auditLog.setEntityType("ApiClient");
        auditLog.setEntityId(apiClient.getId());
        auditLog.setReason("secret_rotated");
        auditLogRepository.save(auditLog);

        return new ApiClientCredential(clientId, secret);
    }

    @Transactional
    public void revoke(String clientId, String reason) {
        ApiClient apiClient = apiClientRepository.findByClientId(clientId).orElseThrow();
        if (apiClient.getStatus() == ApiClientStatus.REVOKED) {
            return;
        }
        apiClient.setStatus(ApiClientStatus.REVOKED);
        apiClient.setRevokedAt(Instant.now());
        apiClient.setRevokedReason(reason);
        apiClientRepository.save(apiClient);

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(AuditAction.UPDATE);
        auditLog.setEntityType("ApiClient");
        auditLog.setEntityId(apiClient.getId());
        auditLog.setReason(reason);
        auditLogRepository.save(auditLog);
    }

    /** Rejects before any business logic runs — IP/rate-limit failures are not logged or let through. */
    @Transactional
    public Optional<String[]> authenticate(String clientId, String secret, String requestIp) {
        Optional<ApiClient> maybeClient = apiClientRepository.findByClientId(clientId);
        if (maybeClient.isEmpty()) {
            return Optional.empty();
        }
        ApiClient apiClient = maybeClient.get();

        if (apiClient.getStatus() != ApiClientStatus.ACTIVE) {
            return Optional.empty();
        }
        if (apiClient.getExpiresAt() != null && !apiClient.getExpiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        if (apiClient.getRevokedAt() != null) {
            return Optional.empty();
        }
        if (!tokenHasher.hash(secret).equals(apiClient.getClientSecretHash())) {
            return Optional.empty();
        }
        if (apiClient.getAllowedIpRanges() != null && !isIpAllowed(requestIp, apiClient.getAllowedIpRanges())) {
            return Optional.empty();
        }
        if (apiClient.getRateLimitPerMinute() != null && isOverRateLimit(clientId, apiClient.getRateLimitPerMinute())) {
            return Optional.empty();
        }

        apiClient.setLastUsedAt(Instant.now());
        apiClient.setLastUsedIp(requestIp);
        apiClientRepository.save(apiClient);

        return Optional.of(apiClient.getAllowedScopes().split("\\s+"));
    }

    private boolean isIpAllowed(String requestIp, String cidrRangesCsv) {
        for (String cidr : cidrRangesCsv.split("[,\\s]+")) {
            if (!cidr.isBlank() && CidrMatcher.matches(requestIp, cidr.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isOverRateLimit(String clientId, int rateLimitPerMinute) {
        Instant now = Instant.now();
        var window = requestWindows.computeIfAbsent(clientId, k -> new java.util.concurrent.ConcurrentLinkedDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(now.minusSeconds(60))) {
                window.pollFirst();
            }
            if (window.size() >= rateLimitPerMinute) {
                return true;
            }
            window.addLast(now);
        }
        return false;
    }
}
