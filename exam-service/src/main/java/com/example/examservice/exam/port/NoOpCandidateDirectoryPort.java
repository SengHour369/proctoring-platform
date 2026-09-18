package com.example.examservice.exam.port;

import com.example.examservice.exam.enums.InvitationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default {@link CandidateDirectoryPort}: logs and optimistically reports the candidate as an
 * active student with a placeholder contact address, instead of calling {@code identity-service}.
 * Replace this bean with a real client so a suspended or non-student candidate is actually
 * rejected at assignment time, and invitations reach a real address.
 */
@Component
public class NoOpCandidateDirectoryPort implements CandidateDirectoryPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpCandidateDirectoryPort.class);

    @Override
    public boolean isActiveStudent(Long candidateUserId) {
        log.warn("[stub candidate-directory] would verify candidate {} is an ACTIVE user with a STUDENT grant "
                + "in identity-service — assuming true", candidateUserId);
        return true;
    }

    @Override
    public Optional<String> resolveContactAddress(Long candidateUserId, InvitationChannel channel) {
        log.warn("[stub candidate-directory] would resolve candidate {}'s {} address in identity-service",
                candidateUserId, channel);
        return Optional.of("candidate-" + candidateUserId + "@unresolved.local");
    }
}
