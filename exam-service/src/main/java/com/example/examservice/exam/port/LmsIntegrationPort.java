package com.example.examservice.exam.port;

import java.util.Optional;

/**
 * The call this module makes into an external LMS to evaluate a coursework-based
 * {@code ExamPrerequisite} rule. Empty means the integration is unavailable, which the caller must
 * treat as a failed rule (fail closed) rather than assumed completion — see
 * {@link NoOpLmsIntegrationPort} for the stub used until a real client is wired in.
 */
public interface LmsIntegrationPort {

    Optional<Boolean> isCourseworkComplete(Long candidateUserId, String courseReference);
}
