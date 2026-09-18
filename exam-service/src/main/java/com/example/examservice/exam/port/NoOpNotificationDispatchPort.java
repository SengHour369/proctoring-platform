package com.example.examservice.exam.port;

import com.example.examservice.exam.enums.InvitationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link NotificationDispatchPort}: logs and reports success, instead of calling
 * {@code notification-service}. Replace this bean with a real client so an invitation is actually
 * delivered.
 */
@Component
public class NoOpNotificationDispatchPort implements NotificationDispatchPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpNotificationDispatchPort.class);

    @Override
    public DispatchResult dispatchInvitation(Long candidateUserId, String sentTo, InvitationChannel channel,
                                              String plaintextToken) {
        log.info("[stub notification-dispatch] would send {} invitation to {} (candidate {}) via "
                + "notification-service", channel, sentTo, candidateUserId);
        return DispatchResult.ok();
    }
}
