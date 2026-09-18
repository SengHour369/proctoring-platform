package com.example.examservice.common.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple in-process failed-attempt counter, keyed by an arbitrary string (e.g. an assignment
 * id), for endpoints that need the same "too many failures, back off" shape as
 * {@code AuthService.login}'s lockout without a dedicated persisted counter column. Single-instance
 * only — a multi-instance deployment needs a shared store (e.g. Redis) instead.
 */
@Component
public class PerKeyRateLimiter {

    private record Window(AtomicInteger failures, Instant windowStart) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean isLocked(String key, int maxFailures, Duration windowDuration) {
        Window window = windows.get(key);
        if (window == null) {
            return false;
        }
        if (Instant.now().isAfter(window.windowStart().plus(windowDuration))) {
            windows.remove(key, window);
            return false;
        }
        return window.failures().get() >= maxFailures;
    }

    public void recordFailure(String key, Duration windowDuration) {
        windows.compute(key, (k, existing) -> {
            if (existing == null || Instant.now().isAfter(existing.windowStart().plus(windowDuration))) {
                return new Window(new AtomicInteger(1), Instant.now());
            }
            existing.failures().incrementAndGet();
            return existing;
        });
    }

    public void reset(String key) {
        windows.remove(key);
    }
}
