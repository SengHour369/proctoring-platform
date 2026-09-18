package com.example.identityservice.user.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link ReviewCaseReleasePort}: logs instead of calling {@code review-service}. Replace
 * this bean so a disabled reviewer's open cases are actually returned to the queue.
 */
@Component
public class NoOpReviewCaseReleasePort implements ReviewCaseReleasePort {

    private static final Logger log = LoggerFactory.getLogger(NoOpReviewCaseReleasePort.class);

    @Override
    public void releaseCasesAssignedTo(Long reviewerUserId) {
        log.info("[stub review-case-release] would release cases assigned to reviewer {}", reviewerUserId);
    }
}
