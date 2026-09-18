package com.example.examservice.exam.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link ReviewDecisionPort}: logs and fails closed, instead of calling
 * {@code review-service}. A retake grant must never be created on an unverified decision, so this
 * stub reports false until a real client is wired in.
 */
@Component
public class NoOpReviewDecisionPort implements ReviewDecisionPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpReviewDecisionPort.class);

    @Override
    public boolean isGrantRetakeDecision(Long reviewDecisionId) {
        log.warn("[stub review-decision] would verify decision {} is GRANT_RETAKE in review-service — "
                + "failing closed", reviewDecisionId);
        return false;
    }
}
