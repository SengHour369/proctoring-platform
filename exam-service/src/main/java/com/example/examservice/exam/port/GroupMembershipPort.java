package com.example.examservice.exam.port;

import java.util.List;
import java.util.Optional;

/**
 * The calls this module makes into {@code identity-service} to resolve a {@code StudentGroup} and
 * its active {@code GroupMembership} rows. Group and membership data live in identity-service's
 * own schema, not here — see {@link NoOpGroupMembershipPort} for the stub used until a real client
 * is wired in.
 */
public interface GroupMembershipPort {

    Optional<GroupSummary> findGroup(Long studentGroupId);

    /** User ids of every active member of the group, for fan-out expansion. */
    List<Long> findActiveMemberUserIds(Long studentGroupId);

    record GroupSummary(Long id, boolean active, String groupType) {
    }
}
