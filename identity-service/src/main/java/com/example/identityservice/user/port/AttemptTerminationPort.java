package com.example.identityservice.user.port;

/**
 * The one call this module makes into {@code attempt-service} when an account is disabled: end
 * whatever exam attempt is currently in flight for that candidate, through the same
 * {@code ExamAttemptService.terminateAttempt} path used everywhere else an attempt is cut short.
 * Attempt data lives in a different microservice's schema — see {@link NoOpAttemptTerminationPort}
 * for the stub used until a real client is wired in.
 */
public interface AttemptTerminationPort {

    /** Terminates every {@code NOT_STARTED}/{@code IN_PROGRESS}/{@code PAUSED} attempt for the candidate. */
    void terminateInFlightAttempts(Long candidateUserId, String reason);
}
