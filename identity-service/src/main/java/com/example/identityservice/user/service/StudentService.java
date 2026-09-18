package com.example.identityservice.user.service;

import com.example.identityservice.auth.dto.RequestContext;
import com.example.identityservice.auth.enums.RoleCode;
import com.example.identityservice.auth.exception.AccessDeniedException;
import com.example.identityservice.auth.service.RoleService;
import com.example.identityservice.user.config.RoleProfile;
import com.example.identityservice.user.dto.CreateUserCommand;
import com.example.identityservice.user.dto.CreatedAccount;
import com.example.identityservice.user.dto.UpdateUserCommand;
import com.example.identityservice.user.dto.UserProfileView;
import com.example.identityservice.user.enums.DisableMode;
import org.springframework.stereotype.Service;

/**
 * §2.1 — Student Management. A thin, role-named façade over {@link UserLifecycleService} and
 * {@link UserProfileService}: everything role-specific lives in the {@link #PROFILE} it hands
 * them, plus the teacher-scoped viewing rule in {@link #getProfile}.
 */
@Service
public class StudentService {

    private static final RoleProfile PROFILE = new RoleProfile(
            RoleCode.STUDENT,
            "student:create", "student:update", "student:disable", "student:view",
            "profile:update_own", "profile:view_own",
            true,   // supportsGroupMembership
            false,  // grantsExpireByDefault
            null,
            false,  // blockSelfDisable
            false,  // requireSecondApprover
            true,   // terminatesAttemptsOnDisable
            false); // releasesReviewCasesOnDisable

    /** A teacher without full {@code student:view} may still see a student in their own class. */
    private static final String TEACHER_CLASS_VIEW_PERMISSION = "student:view_class";

    private final UserLifecycleService lifecycleService;
    private final UserProfileService profileService;
    private final RoleService roleService;
    private final GroupScopeChecker groupScopeChecker;

    public StudentService(
            UserLifecycleService lifecycleService,
            UserProfileService profileService,
            RoleService roleService,
            GroupScopeChecker groupScopeChecker) {
        this.lifecycleService = lifecycleService;
        this.profileService = profileService;
        this.roleService = roleService;
        this.groupScopeChecker = groupScopeChecker;
    }

    public CreatedAccount createStudent(CreateUserCommand command, Long callerUserId, RequestContext ctx) {
        return lifecycleService.createUser(PROFILE, command, callerUserId, null, ctx);
    }

    public void updateStudent(Long userId, UpdateUserCommand command, Long callerUserId, RequestContext ctx) {
        lifecycleService.updateUser(PROFILE, userId, command, callerUserId, null, ctx);
    }

    public void disableStudent(Long userId, DisableMode mode, String reason, Long callerUserId) {
        lifecycleService.disableUser(PROFILE, userId, mode, reason, callerUserId);
    }

    public UserProfileView getProfile(Long userId, Long callerUserId) {
        if (profileService.isAuthorizedToView(PROFILE, userId, callerUserId)) {
            return profileService.buildProfile(userId, callerUserId);
        }
        boolean teacherInScope = roleService.checkAccess(callerUserId, TEACHER_CLASS_VIEW_PERMISSION)
                && groupScopeChecker.teacherSharesGroupWithStudent(callerUserId, userId);
        if (!teacherInScope) {
            throw new AccessDeniedException("Caller lacks permission to view this student");
        }
        return profileService.buildProfile(userId, callerUserId);
    }
}
