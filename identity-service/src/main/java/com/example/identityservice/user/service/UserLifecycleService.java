package com.example.identityservice.user.service;

import com.example.identityservice.auth.audit.AuditWriter;
import com.example.identityservice.auth.audit.JsonSnapshot;
import com.example.identityservice.auth.audit.enums.AuditAction;
import com.example.identityservice.auth.audit.enums.AuditOutcome;
import com.example.identityservice.auth.config.AuthProperties;
import com.example.identityservice.auth.dto.RequestContext;
import com.example.identityservice.auth.entity.Role;
import com.example.identityservice.auth.entity.SecurityToken;
import com.example.identityservice.auth.entity.User;
import com.example.identityservice.auth.entity.UserRole;
import com.example.identityservice.auth.entity.UserSession;
import com.example.identityservice.auth.enums.TokenPurpose;
import com.example.identityservice.auth.enums.UserStatus;
import com.example.identityservice.auth.exception.AccessDeniedException;
import com.example.identityservice.auth.exception.DuplicateValueException;
import com.example.identityservice.auth.notification.EmailSender;
import com.example.identityservice.auth.repository.RoleRepository;
import com.example.identityservice.auth.repository.SecurityTokenRepository;
import com.example.identityservice.auth.repository.UserRepository;
import com.example.identityservice.auth.repository.UserRoleRepository;
import com.example.identityservice.auth.repository.UserSessionRepository;
import com.example.identityservice.auth.security.TokenHasher;
import com.example.identityservice.auth.service.RoleService;
import com.example.identityservice.user.config.RoleProfile;
import com.example.identityservice.user.dto.CreateUserCommand;
import com.example.identityservice.user.dto.CreatedAccount;
import com.example.identityservice.user.dto.UpdateUserCommand;
import com.example.identityservice.user.enums.DisableMode;
import com.example.identityservice.user.port.AttemptTerminationPort;
import com.example.identityservice.user.port.GroupAutoEnrollPort;
import com.example.identityservice.user.port.ReviewCaseReleasePort;
import com.example.identityservice.usergroup.entity.GroupMembership;
import com.example.identityservice.usergroup.entity.StudentGroup;
import com.example.identityservice.usergroup.repository.GroupMembershipRepository;
import com.example.identityservice.usergroup.repository.StudentGroupRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The one CRUD engine behind {@code StudentService}, {@code TeacherService}, {@code
 * ReviewerService} and {@code AdminService} — see §2's "shared management contract". Each of
 * those four is a thin, role-named façade that builds a {@link RoleProfile} and delegates here;
 * this class holds the create/update/disable steps common to all of them so that logic exists
 * exactly once.
 */
@Service
public class UserLifecycleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final SecurityTokenRepository securityTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final RoleService roleService;
    private final AuditWriter auditWriter;
    private final JsonSnapshot jsonSnapshot;
    private final TokenHasher tokenHasher;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final GroupAutoEnrollPort groupAutoEnrollPort;
    private final AttemptTerminationPort attemptTerminationPort;
    private final ReviewCaseReleasePort reviewCaseReleasePort;
    private final AuthProperties properties;

    public UserLifecycleService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            GroupMembershipRepository groupMembershipRepository,
            StudentGroupRepository studentGroupRepository,
            SecurityTokenRepository securityTokenRepository,
            UserSessionRepository userSessionRepository,
            RoleService roleService,
            AuditWriter auditWriter,
            JsonSnapshot jsonSnapshot,
            TokenHasher tokenHasher,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            GroupAutoEnrollPort groupAutoEnrollPort,
            AttemptTerminationPort attemptTerminationPort,
            ReviewCaseReleasePort reviewCaseReleasePort,
            AuthProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.securityTokenRepository = securityTokenRepository;
        this.userSessionRepository = userSessionRepository;
        this.roleService = roleService;
        this.auditWriter = auditWriter;
        this.jsonSnapshot = jsonSnapshot;
        this.tokenHasher = tokenHasher;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.groupAutoEnrollPort = groupAutoEnrollPort;
        this.attemptTerminationPort = attemptTerminationPort;
        this.reviewCaseReleasePort = reviewCaseReleasePort;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ create

    /**
     * §2.1.1 in full generality. {@code secondApproverUserId} is only consulted when {@code
     * profile.requireSecondApprover()} is set (ADMIN) — pass {@code null} for every other family.
     */
    @Transactional
    public CreatedAccount createUser(
            RoleProfile profile, CreateUserCommand cmd, Long callerUserId, Long secondApproverUserId, RequestContext ctx) {
        requirePermissionOrDenyAudited(callerUserId, profile.createPermission(), "User", null, "missing " + profile.createPermission());

        if (profile.requireSecondApprover()) {
            requireDistinctSecondApprover(callerUserId, secondApproverUserId, profile.createPermission());
        }

        String normalizedEmail = normalizeEmail(cmd.email());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateValueException("email already in use");
        }
        if (cmd.externalRef() != null && userRepository.findByExternalRef(cmd.externalRef()).isPresent()) {
            throw new DuplicateValueException("external reference already in use");
        }

        StudentGroup group = null;
        if (profile.supportsGroupMembership() && cmd.groupId() != null) {
            group = studentGroupRepository.findById(cmd.groupId())
                    .orElseThrow(() -> new IllegalArgumentException("groupId does not refer to an existing group"));
            if (!group.isActive()) {
                throw new IllegalArgumentException("groupId refers to an inactive group");
            }
        }

        Role role = roleRepository.findByCode(profile.roleCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Platform misconfiguration: role " + profile.roleCode() + " is not seeded"));

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(cmd.fullName());
        user.setExternalRef(cmd.externalRef());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setPasswordHash(passwordEncoder.encode(tokenHasher.generateToken()));
        user.setFailedLoginCount(0);
        user.setMfaEnabled(false);
        user.setPhoneNumber(cmd.phoneNumber());
        user.setTimeZone(cmd.timeZone());
        user.setLocale(cmd.locale());
        user = userRepository.save(user);

        Instant expiresAt = resolveGrantExpiry(profile, callerUserId, cmd.roleExpiresAt());
        UserRole grant = new UserRole();
        grant.setUserId(user.getId());
        grant.setRoleId(role.getId());
        grant.setGrantedByUserId(callerUserId);
        grant.setGrantedAt(Instant.now());
        grant.setExpiresAt(expiresAt);
        grant = userRoleRepository.save(grant);

        auditWriter.write(callerUserId, AuditAction.PERMISSION_CHANGE, "UserRole", grant.getId(),
                null, jsonSnapshot.of(Map.of("userId", user.getId(), "roleCode", profile.roleCode())),
                profile.roleCode().toLowerCase() + "_created");

        if (group != null) {
            GroupMembership membership = new GroupMembership();
            membership.setStudentGroupId(group.getId());
            membership.setUserId(user.getId());
            membership.setAddedByUserId(callerUserId);
            membership.setJoinedAt(Instant.now());
            membership.setActive(true);
            groupMembershipRepository.save(membership);
            groupAutoEnrollPort.enrollNewGroupMember(group.getId(), user.getId());
        }

        String token = tokenHasher.generateToken();
        SecurityToken invitation = new SecurityToken();
        invitation.setUserId(user.getId());
        invitation.setPurpose(TokenPurpose.ACCOUNT_INVITATION);
        invitation.setTokenHash(tokenHasher.hash(token));
        invitation.setIssuedAt(Instant.now());
        invitation.setExpiresAt(Instant.now().plus(properties.getAccountInvitationTokenTtl()));
        invitation.setRequestedIp(ctx == null ? null : ctx.ipAddress());
        securityTokenRepository.save(invitation);
        emailSender.sendAccountInvitation(user.getEmail(), token);

        Map<String, Object> after = new HashMap<>();
        after.put("status", user.getStatus().name());
        after.put("email", user.getEmail());
        after.put("fullName", user.getFullName());
        after.put("externalRef", user.getExternalRef());
        auditWriter.write(callerUserId, AuditAction.CREATE, "User", user.getId(), null,
                jsonSnapshot.of(after), profile.roleCode().toLowerCase() + "_created");

        if (profile.requireSecondApprover()) {
            auditWriter.write(secondApproverUserId, AuditAction.PERMISSION_CHANGE, "User", user.getId(),
                    null, null, "admin_creation_confirmed");
        }

        return new CreatedAccount(user.getPublicId(), user.getEmail(), user.getStatus());
    }

    // ------------------------------------------------------------------ update

    /** §2.1.2 in full generality. */
    @Transactional
    public void updateUser(
            RoleProfile profile, Long targetUserId, UpdateUserCommand cmd, Long callerUserId,
            Long secondApproverUserId, RequestContext ctx) {
        boolean actingOnSelf = callerUserId.equals(targetUserId);
        boolean allowed = roleService.checkAccess(callerUserId, profile.updatePermission())
                || (actingOnSelf && profile.selfUpdatePermission() != null
                    && roleService.checkAccess(callerUserId, profile.selfUpdatePermission()));
        if (!allowed) {
            auditWriter.writeOutcome(callerUserId, AuditAction.UPDATE, AuditOutcome.DENIED, "User", targetUserId,
                    "missing " + profile.updatePermission());
            throw new AccessDeniedException("Caller lacks permission to update this user");
        }

        User user = userRepository.findById(targetUserId).orElseThrow();
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new IllegalStateException("A disabled account cannot be edited through the normal path");
        }

        boolean emailChanging = cmd.getEmail() != null && !normalizeEmail(cmd.getEmail()).equalsIgnoreCase(user.getEmail());
        boolean externalRefChanging = cmd.getExternalRef() != null && !cmd.getExternalRef().equals(user.getExternalRef());

        if (profile.requireSecondApprover() && actingOnSelf && emailChanging) {
            requireDistinctSecondApprover(callerUserId, secondApproverUserId, profile.updatePermission());
        }
        if ((emailChanging || externalRefChanging) && (cmd.getReason() == null || cmd.getReason().isBlank())) {
            throw new IllegalArgumentException("reason is required when changing email or externalRef");
        }

        String normalizedNewEmail = emailChanging ? normalizeEmail(cmd.getEmail()) : null;
        if (emailChanging && userRepository.findByEmail(normalizedNewEmail).isPresent()) {
            throw new DuplicateValueException("email already in use");
        }
        if (externalRefChanging && userRepository.findByExternalRef(cmd.getExternalRef()).isPresent()) {
            throw new DuplicateValueException("external reference already in use");
        }

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (cmd.getFullName() != null) {
            before.put("fullName", user.getFullName());
            user.setFullName(cmd.getFullName());
            after.put("fullName", user.getFullName());
        }
        if (cmd.getPhoneNumber() != null) {
            before.put("phoneNumber", user.getPhoneNumber());
            user.setPhoneNumber(cmd.getPhoneNumber());
            after.put("phoneNumber", user.getPhoneNumber());
        }
        if (cmd.getTimeZone() != null) {
            before.put("timeZone", user.getTimeZone());
            user.setTimeZone(cmd.getTimeZone());
            after.put("timeZone", user.getTimeZone());
        }
        if (cmd.getLocale() != null) {
            before.put("locale", user.getLocale());
            user.setLocale(cmd.getLocale());
            after.put("locale", user.getLocale());
        }
        if (cmd.getEnrolmentPhotoPath() != null) {
            user.setEnrolmentPhotoPath(cmd.getEnrolmentPhotoPath());
        }
        if (cmd.getVoiceprintPath() != null) {
            user.setVoiceprintPath(cmd.getVoiceprintPath());
        }
        if (externalRefChanging) {
            before.put("externalRef", user.getExternalRef());
            user.setExternalRef(cmd.getExternalRef());
            after.put("externalRef", user.getExternalRef());
        }

        if (emailChanging) {
            before.put("email", user.getEmail());
            String oldEmail = user.getEmail();
            user.setEmail(normalizedNewEmail);
            user.setEmailVerifiedAt(null);
            after.put("email", user.getEmail());

            String token = tokenHasher.generateToken();
            SecurityToken changeToken = new SecurityToken();
            changeToken.setUserId(user.getId());
            changeToken.setPurpose(TokenPurpose.EMAIL_CHANGE);
            changeToken.setTokenHash(tokenHasher.hash(token));
            changeToken.setIssuedAt(Instant.now());
            changeToken.setExpiresAt(Instant.now().plus(properties.getEmailChangeTokenTtl()));
            changeToken.setRequestedIp(ctx == null ? null : ctx.ipAddress());
            securityTokenRepository.save(changeToken);

            emailSender.sendEmailChangeConfirmation(normalizedNewEmail, token);
            emailSender.sendEmailChangeNotice(oldEmail, normalizedNewEmail);
        }

        userRepository.save(user);

        auditWriter.write(callerUserId, AuditAction.UPDATE, "User", user.getId(),
                jsonSnapshot.of(before), jsonSnapshot.of(after), cmd.getReason());
    }

    // ------------------------------------------------------------------ disable

    /** §2.1.3 in full generality. */
    @Transactional
    public void disableUser(RoleProfile profile, Long targetUserId, DisableMode mode, String reason, Long callerUserId) {
        boolean allowed = roleService.checkAccess(callerUserId, profile.disablePermission());
        if (!allowed) {
            auditWriter.writeOutcome(callerUserId, AuditAction.UPDATE, AuditOutcome.DENIED, "User", targetUserId,
                    "missing " + profile.disablePermission());
            throw new AccessDeniedException("Caller lacks permission to disable this user");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (profile.blockSelfDisable() && callerUserId.equals(targetUserId)) {
            throw new IllegalStateException("This account cannot disable itself");
        }

        User user = userRepository.findById(targetUserId).orElseThrow();

        Role role = roleRepository.findByCode(profile.roleCode())
                .orElseThrow(() -> new IllegalStateException("Platform misconfiguration: role " + profile.roleCode() + " is not seeded"));
        if (userRoleRepository.findByUserIdAndRoleId(targetUserId, role.getId()).isEmpty()) {
            throw new IllegalArgumentException("Target user has never held " + profile.roleCode());
        }

        UserStatus targetStatus = mode == DisableMode.DISABLED ? UserStatus.DISABLED : UserStatus.SUSPENDED;
        if (user.getStatus() == targetStatus) {
            return; // idempotent no-op
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(targetStatus);
        userRepository.save(user);

        List<UserSession> sessions = userSessionRepository.findByUserIdAndRevokedAtIsNull(targetUserId);
        Instant now = Instant.now();
        String revokedReason = mode == DisableMode.DISABLED ? "account_disabled" : "account_suspended";
        for (UserSession session : sessions) {
            session.setRevokedAt(now);
            session.setRevokedReason(revokedReason);
        }
        userSessionRepository.saveAll(sessions);

        List<UserRole> grants = userRoleRepository.findByUserId(targetUserId);
        for (UserRole grant : grants) {
            if (grant.getExpiresAt() == null || grant.getExpiresAt().isAfter(now)) {
                Instant oldExpiry = grant.getExpiresAt();
                grant.setExpiresAt(now);
                userRoleRepository.save(grant);
                auditWriter.write(callerUserId, AuditAction.PERMISSION_CHANGE, "UserRole", grant.getId(),
                        jsonSnapshot.of(mapOf("expiresAt", oldExpiry)),
                        jsonSnapshot.of(mapOf("expiresAt", now)), revokedReason);
            }
        }

        List<GroupMembership> memberships = groupMembershipRepository.findByUserIdAndActiveTrue(targetUserId);
        for (GroupMembership membership : memberships) {
            membership.setActive(false);
            membership.setLeftAt(now);
            groupMembershipRepository.save(membership);
            auditWriter.write(callerUserId, AuditAction.UPDATE, "GroupMembership", membership.getId(),
                    null, null, revokedReason);
        }

        if (profile.terminatesAttemptsOnDisable()) {
            attemptTerminationPort.terminateInFlightAttempts(targetUserId, revokedReason);
        }
        if (profile.releasesReviewCasesOnDisable()) {
            reviewCaseReleasePort.releaseCasesAssignedTo(targetUserId);
        }

        auditWriter.write(callerUserId, AuditAction.UPDATE, "User", targetUserId,
                jsonSnapshot.of(mapOf("status", oldStatus)),
                jsonSnapshot.of(mapOf("status", targetStatus)), reason);
    }

    // ------------------------------------------------------------------ helpers

    private void requirePermissionOrDenyAudited(
            Long callerUserId, String permissionCode, String entityType, Long entityId, String reason) {
        if (!roleService.checkAccess(callerUserId, permissionCode)) {
            auditWriter.writeOutcome(callerUserId, AuditAction.CREATE, AuditOutcome.DENIED, entityType, entityId, reason);
            throw new AccessDeniedException("Caller lacks permission: " + permissionCode);
        }
    }

    private void requireDistinctSecondApprover(Long callerUserId, Long secondApproverUserId, String permissionCode) {
        if (secondApproverUserId == null || secondApproverUserId.equals(callerUserId)) {
            throw new IllegalArgumentException("A distinct second admin must confirm this change");
        }
        if (!roleService.checkAccess(secondApproverUserId, permissionCode)) {
            throw new AccessDeniedException("The confirming admin lacks permission: " + permissionCode);
        }
    }

    private Instant resolveGrantExpiry(RoleProfile profile, Long callerUserId, Instant requestedExpiry) {
        if (!profile.grantsExpireByDefault()) {
            return null;
        }
        boolean canSkipExpiry = profile.permanentGrantOverride() != null
                && roleService.checkAccess(callerUserId, profile.permanentGrantOverride());
        if (requestedExpiry == null) {
            if (canSkipExpiry) {
                return null;
            }
            throw new IllegalArgumentException(
                    "A " + profile.roleCode() + " grant must have an expiry unless the caller holds "
                            + profile.permanentGrantOverride());
        }
        if (!requestedExpiry.isAfter(Instant.now())) {
            throw new IllegalArgumentException("roleExpiresAt must be in the future");
        }
        return requestedExpiry;
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase();
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
