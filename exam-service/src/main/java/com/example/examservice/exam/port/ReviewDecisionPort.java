package com.example.examservice.exam.port;

/**
 * The call this module makes into {@code review-service} to confirm a {@code ReviewDecision}
 * exists and carries {@code decisionType = GRANT_RETAKE} before a {@code RetakeGrant} is created —
 * a grant cannot exist without that decision behind it. Decision data lives in review-service's own
 * schema, not here — see {@link NoOpReviewDecisionPort} for the stub used until a real client is
 * wired in.
 */
public interface ReviewDecisionPort {

    boolean isGrantRetakeDecision(Long reviewDecisionId);
}
