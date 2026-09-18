package com.example.examservice.exam.port;

import com.example.examservice.exam.enums.InvitationChannel;

import java.util.Optional;

/**
 * The calls this module makes into {@code identity-service} for candidate identity data: whether
 * a candidate is an {@code ACTIVE} user holding a non-expired {@code STUDENT} grant, and the
 * address/number to use for a given invitation channel. User data lives in identity-service's own
 * schema, not here — see {@link NoOpCandidateDirectoryPort} for the stub used until a real client
 * is wired in.
 */
public interface CandidateDirectoryPort {

    boolean isActiveStudent(Long candidateUserId);

    /** Empty when the candidate has no address/number for that channel (e.g. no phone for SMS). */
    Optional<String> resolveContactAddress(Long candidateUserId, InvitationChannel channel);
}
