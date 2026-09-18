package com.example.examservice.exam.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Default {@link GroupMembershipPort}: logs instead of calling {@code identity-service}. Replace
 * this bean with a real client so group and class assignment can actually fan out to members.
 */
@Component
public class NoOpGroupMembershipPort implements GroupMembershipPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpGroupMembershipPort.class);

    @Override
    public Optional<GroupSummary> findGroup(Long studentGroupId) {
        log.warn("[stub group-membership] would look up student group {} in identity-service", studentGroupId);
        return Optional.empty();
    }

    @Override
    public List<Long> findActiveMemberUserIds(Long studentGroupId) {
        log.warn("[stub group-membership] would fetch active members of student group {} from identity-service",
                studentGroupId);
        return List.of();
    }
}
