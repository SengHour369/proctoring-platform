package com.example.identityservice.user.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link AttemptTerminationPort}: logs instead of calling {@code attempt-service}. Replace
 * this bean with a real client so a disabled candidate's in-flight attempt is actually terminated.
 */
@Component
public class NoOpAttemptTerminationPort implements AttemptTerminationPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpAttemptTerminationPort.class);

    @Override
    public void terminateInFlightAttempts(Long candidateUserId, String reason) {
        log.info("[stub attempt-termination] would terminate in-flight attempts for candidate {} ({})",
                candidateUserId, reason);
    }
}
