package com.example.identityservice.user.dto;

import java.time.Instant;

/**
 * One row of the §2.5 activity read model, after {@code audit_logs}/{@code login_attempts}/
 * {@code user_sessions} have been merged and sorted. {@code source} names which table it came
 * from, so a caller who is masked out of one source never sees it appear here either.
 */
public record ActivityEntry(Instant occurredAt, String source, String action, String detail) {
}
