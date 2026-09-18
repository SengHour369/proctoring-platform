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
 * §2.2 — Teacher Management. Same shape as {@link StudentService}; the deltas are the
 * {@code teacher:*} permission codes and {@code blockSelfDisable = true} — a teacher cannot
 * disable their own account.
 */
@Service
public class TeacherService {

    private static final RoleProfile PROFILE = new RoleProfile(
            RoleCode.TEACHER,
            "teacher:create", "teacher:update", "teacher:disable", "teacher:view",
            "profile:update_own", "profile:view_own",
            false,  // supportsGroupMembership — a teacher owns/teaches groups, isn't a member of one
            false,  // grantsExpireByDefault
            null,
            true,   // blockSelfDisable
            false,  // requireSecondApprover
            false,  // terminatesAttemptsOnDisable
            false); // releasesReviewCasesOnDisable

    private final UserLifecycleService lifecycleService;
    private final UserProfileService profileService;

    public TeacherService(UserLifecycleService lifecycleService, UserProfileService profileService) {
        this.lifecycleService = lifecycleService;
        this.profileService = profileService;
    }

    public CreatedAccount createTeacher(CreateUserCommand command, Long callerUserId, RequestContext ctx) {
        return lifecycleService.createUser(PROFILE, command, callerUserId, null, ctx);
    }

    public void updateTeacher(Long userId, UpdateUserCommand command, Long callerUserId, RequestContext ctx) {
        lifecycleService.updateUser(PROFILE, userId, command, callerUserId, null, ctx);
    }

    public void disableTeacher(Long userId, DisableMode mode, String reason, Long callerUserId) {
        lifecycleService.disableUser(PROFILE, userId, mode, reason, callerUserId);
    }

    public UserProfileView getProfile(Long userId, Long callerUserId) {
        return profileService.getProfile(PROFILE, userId, callerUserId);
    }
}
