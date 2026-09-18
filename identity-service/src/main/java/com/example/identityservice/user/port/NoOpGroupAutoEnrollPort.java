package com.example.identityservice.user.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link GroupAutoEnrollPort}: logs instead of calling {@code exam-service}. Replace this
 * bean with a real client so a new group member is actually auto-enrolled.
 */
@Component
public class NoOpGroupAutoEnrollPort implements GroupAutoEnrollPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpGroupAutoEnrollPort.class);

    @Override
    public void enrollNewGroupMember(Long studentGroupId, Long candidateUserId) {
        log.info("[stub group-auto-enroll] would evaluate auto-enroll exams in group {} for candidate {}",
                studentGroupId, candidateUserId);
    }
}
