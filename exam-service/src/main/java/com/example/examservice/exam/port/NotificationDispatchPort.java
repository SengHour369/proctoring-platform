package com.example.examservice.exam.port;

import com.example.examservice.exam.enums.InvitationChannel;

/**
 * The call this module makes into {@code notification-service} to actually deliver an exam
 * invitation. Template rendering and delivery state live in notification-service's own schema, not
 * here — see {@link NoOpNotificationDispatchPort} for the stub used until a real client is wired
 * in.
 */
public interface NotificationDispatchPort {

    DispatchResult dispatchInvitation(Long candidateUserId, String sentTo, InvitationChannel channel,
                                       String plaintextToken);

    record DispatchResult(boolean success, String failureReason) {

        public static DispatchResult ok() {
            return new DispatchResult(true, null);
        }

        public static DispatchResult failed(String reason) {
            return new DispatchResult(false, reason);
        }
    }
}
