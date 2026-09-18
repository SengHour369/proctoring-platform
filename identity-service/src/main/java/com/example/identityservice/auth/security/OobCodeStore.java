package com.example.identityservice.auth.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, in-memory holder for the one-time code sent to a user's email for
 * {@code MfaMethod.EMAIL}. Only the hash is kept, matching the same reasoning as
 * {@link TokenHasher} — nothing about this code should be recoverable from a memory dump.
 */
@Component
public class OobCodeStore {

    private record Entry(String codeHash, Instant expiresAt) {
    }

    private final Map<Long, Entry> codes = new ConcurrentHashMap<>();
    private final TokenHasher tokenHasher;
    private final SecureRandom secureRandom;

    public OobCodeStore(TokenHasher tokenHasher, SecureRandom secureRandom) {
        this.tokenHasher = tokenHasher;
        this.secureRandom = secureRandom;
    }

    /** Generates and stores a 6-digit code, returning the plaintext for the caller to send. */
    public String issue(Long userId, Duration ttl) {
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        codes.put(userId, new Entry(tokenHasher.hash(code), Instant.now().plus(ttl)));
        return code;
    }

    public boolean verify(Long userId, String code) {
        Entry entry = codes.get(userId);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return false;
        }
        return entry.codeHash().equals(tokenHasher.hash(code));
    }

    /** Consumed only once the code actually matches — a wrong attempt may be retried. */
    public void consume(Long userId) {
        codes.remove(userId);
    }
}
