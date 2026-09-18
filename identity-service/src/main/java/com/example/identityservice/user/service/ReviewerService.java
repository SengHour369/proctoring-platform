package com.example.identityservice.user.service;

import com.example.identityservice.auth.dto.RequestContext;
import com.example.identityservice.auth.enums.RoleCode;
import com.example.identityservice.user.config.RoleProfile;
import com.example.identityservice.user.dto.CreateUserCommand;
import com.example.identityservice.user.dto.CreatedAccount;
import com.example.identityservice.user.dto.UpdateUserCommand;
import com.example.identityservice.user.dto.UserProfileView;
import com.example.identityservice.user.enums.DisableMode;
import org.springframework.stereotype.Service;

/**
 * §2.3 — Reviewer Management. Grants default to time-boxed: {@code createReviewer} requires
 * {@link CreateUserCommand#roleExpiresAt()} unless the caller holds {@code reviewer:grant_permanent}.
 * Disabling a reviewer also releases their open {@code review_cases} back to the queue, via
 * {@link com.example.identityservice.user.port.ReviewCaseReleasePort}.
 */
@Service
public class ReviewerService {

    private static final RoleProfile PROFILE = new RoleProfile(
            RoleCode.REVIEWER,
            "reviewer:create", "reviewer:update", "reviewer:disable", "reviewer:view",
            "profile:update_own", "profile:view_own",
            false,  // supportsGroupMembership
            true,   // grantsExpireByDefault
            "reviewer:grant_permanent",
            false,  // blockSelfDisable
            false,  // requireSecondApprover
            false,  // terminatesAttemptsOnDisable
            true);  // releasesReviewCasesOnDisable

    private final UserLifecycleService lifecycleService;
    private final UserProfileService profileService;

    public ReviewerService(UserLifecycleService lifecycleService, UserProfileService profileService) {
        this.lifecycleService = lifecycleService;
        this.profileService = profileService;
    }

    public CreatedAccount createReviewer(CreateUserCommand command, Long callerUserId, RequestContext ctx) {
        return lifecycleService.createUser(PROFILE, command, callerUserId, null, ctx);
    }

    public void updateReviewer(Long userId, UpdateUserCommand command, Long callerUserId, RequestContext ctx) {
        lifecycleService.updateUser(PROFILE, userId, command, callerUserId, null, ctx);
    }

    public void disableReviewer(Long userId, DisableMode mode, String reason, Long callerUserId) {
        lifecycleService.disableUser(PROFILE, userId, mode, reason, callerUserId);
    }

    public UserProfileView getProfile(Long userId, Long callerUserId) {
        return profileService.getProfile(PROFILE, userId, callerUserId);
    }
}
