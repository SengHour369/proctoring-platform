package com.example.identityservice.auth.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, in-memory holder for the "first factor passed" challenge issued between login and
 * {@code verifyMfaCode(userId, code)}. Keyed by user id, not by an opaque token, matching that
 * signature. Deliberately not a {@code SecurityToken} row — it must not be replayable or durable.
 */
@Component
public class MfaChallengeStore {

    private final Map<Long, Instant> challenges = new ConcurrentHashMap<>();

    public void issue(Long userId, Duration ttl) {
        challenges.put(userId, Instant.now().plus(ttl));
    }

    public boolean hasLiveChallenge(Long userId) {
        Instant expiresAt = challenges.get(userId);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    /** Consumed only once the second factor actually succeeds — a wrong code may be retried. */
    public void consume(Long userId) {
        challenges.remove(userId);
    }
}
