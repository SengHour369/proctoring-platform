package com.example.identityservice.user.service;

import com.example.identityservice.auth.dto.RequestContext;
import com.example.identityservice.auth.entity.Role;
import com.example.identityservice.auth.entity.UserRole;
import com.example.identityservice.auth.enums.RoleCode;
import com.example.identityservice.auth.repository.RoleRepository;
import com.example.identityservice.auth.repository.UserRoleRepository;
import com.example.identityservice.user.config.RoleProfile;
import com.example.identityservice.user.dto.CreateUserCommand;
import com.example.identityservice.user.dto.CreatedAccount;
import com.example.identityservice.user.dto.UpdateUserCommand;
import com.example.identityservice.user.dto.UserProfileView;
import com.example.identityservice.user.enums.DisableMode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * §2.4 — Admin Management. Highest privilege, most heavily audited:
 * <ul>
 *   <li>{@code createAdmin} requires dual approval — {@code confirmingAdminUserId} must be a
 *       distinct admin who also holds {@code admin:create}.</li>
 *   <li>An admin cannot disable themselves ({@code blockSelfDisable}), and cannot change their
 *       own email without a second admin confirming ({@code requireSecondApprover}).</li>
 *   <li>The last {@code ADMIN} grant on the platform cannot be disabled away — checked here,
 *       before {@link UserLifecycleService#disableUser} would otherwise close it.</li>
 * </ul>
 * Self-revocation of one's own {@code ADMIN} grant is blocked one layer down, in
 * {@link com.example.identityservice.auth.service.RoleService#revokeRole}.
 */
@Service
public class AdminService {

    private static final RoleProfile PROFILE = new RoleProfile(
            RoleCode.ADMIN,
            "admin:create", "admin:update", "admin:disable", "admin:view",
            "profile:update_own", "profile:view_own",
            false,  // supportsGroupMembership
            false,  // grantsExpireByDefault
            null,
            true,   // blockSelfDisable
            true,   // requireSecondApprover
            false,  // terminatesAttemptsOnDisable
            false); // releasesReviewCasesOnDisable

    private final UserLifecycleService lifecycleService;
    private final UserProfileService profileService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public AdminService(
            UserLifecycleService lifecycleService,
            UserProfileService profileService,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository) {
        this.lifecycleService = lifecycleService;
        this.profileService = profileService;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public CreatedAccount createAdmin(
            CreateUserCommand command, Long initiatingAdminUserId, Long confirmingAdminUserId, RequestContext ctx) {
        return lifecycleService.createUser(PROFILE, command, initiatingAdminUserId, confirmingAdminUserId, ctx);
    }

    public void updateAdmin(
            Long userId, UpdateUserCommand command, Long callerUserId, Long confirmingAdminUserId, RequestContext ctx) {
        lifecycleService.updateUser(PROFILE, userId, command, callerUserId, confirmingAdminUserId, ctx);
    }

    public void disableAdmin(Long userId, DisableMode mode, String reason, Long callerUserId) {
        if (mode == DisableMode.DISABLED) {
            requireNotLastAdmin(userId);
        }
        lifecycleService.disableUser(PROFILE, userId, mode, reason, callerUserId);
    }

    public UserProfileView getProfile(Long userId, Long callerUserId) {
        return profileService.getProfile(PROFILE, userId, callerUserId);
    }

    /** The platform must always retain at least one live ADMIN grant. */
    private void requireNotLastAdmin(Long targetUserId) {
        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Platform misconfiguration: ADMIN role is not seeded"));
        Instant now = Instant.now();
        List<UserRole> grants = userRoleRepository.findByRoleId(adminRole.getId());
        long liveAdminsOtherThanTarget = grants.stream()
                .filter(g -> !g.getUserId().equals(targetUserId))
                .filter(g -> g.getExpiresAt() == null || g.getExpiresAt().isAfter(now))
                .count();
        if (liveAdminsOtherThanTarget == 0) {
            throw new IllegalStateException("The last admin on the platform cannot be disabled");
        }
    }
}
