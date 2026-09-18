package com.example.examservice.exam.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default {@link LmsIntegrationPort}: logs and reports the integration as unavailable, instead of
 * calling a real external LMS. Callers must treat empty as a failed rule.
 */
@Component
public class NoOpLmsIntegrationPort implements LmsIntegrationPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpLmsIntegrationPort.class);

    @Override
    public Optional<Boolean> isCourseworkComplete(Long candidateUserId, String courseReference) {
        log.warn("[stub lms-integration] would check coursework '{}' completion for candidate {}",
                courseReference, candidateUserId);
        return Optional.empty();
    }
}
