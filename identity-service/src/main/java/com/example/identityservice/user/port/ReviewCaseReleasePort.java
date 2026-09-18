package com.example.identityservice.user.port;

/**
 * The one call this module makes into {@code review-service} when a reviewer is disabled: release
 * every case still assigned to them back to the open queue. Review-case data lives in a different
 * microservice's schema — see {@link NoOpReviewCaseReleasePort} for the stub used until a real
 * client is wired in.
 */
public interface ReviewCaseReleasePort {

    /** Clears {@code assignedReviewerUserId} and reopens every case assigned to this reviewer. */
    void releaseCasesAssignedTo(Long reviewerUserId);
}
