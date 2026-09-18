package com.example.identityservice.user.service;

import com.example.identityservice.auth.audit.entity.AuditLog;
import com.example.identityservice.auth.audit.repository.AuditLogRepository;
import com.example.identityservice.auth.entity.Role;
import com.example.identityservice.auth.entity.User;
import com.example.identityservice.auth.entity.UserRole;
import com.example.identityservice.auth.entity.UserSession;
import com.example.identityservice.auth.exception.AccessDeniedException;
import com.example.identityservice.auth.repository.LoginAttemptRepository;
import com.example.identityservice.auth.repository.RoleRepository;
import com.example.identityservice.auth.repository.UserRepository;
import com.example.identityservice.auth.repository.UserRoleRepository;
import com.example.identityservice.auth.repository.UserSessionRepository;
import com.example.identityservice.auth.service.RoleService;
import com.example.identityservice.user.config.RoleProfile;
import com.example.identityservice.user.dto.ActivityEntry;
import com.example.identityservice.user.dto.UserProfileView;
import com.example.identityservice.usergroup.entity.GroupMembership;
import com.example.identityservice.usergroup.entity.StudentGroup;
import com.example.identityservice.usergroup.repository.GroupMembershipRepository;
import com.example.identityservice.usergroup.repository.StudentGroupRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * §2.1.4's read-only profile aggregation, shared by Student/Teacher/Reviewer/Admin the same way
 * {@link UserLifecycleService} shares create/update/disable. No write, no audit — reading a
 * profile is not itself a consequential action.
 */
@Service
public class UserProfileService {

    private static final int RECENT_WINDOW_DAYS = 90;
    private static final int RECENT_LIMIT = 20;

    /** Holds "sensitive" account-health fields back from every caller except an admin. */
    private static final String SENSITIVE_FIELD_PERMISSION = "user:view_sensitive";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final RoleService roleService;

    public UserProfileService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            GroupMembershipRepository groupMembershipRepository,
            StudentGroupRepository studentGroupRepository,
            LoginAttemptRepository loginAttemptRepository,
            UserSessionRepository userSessionRepository,
            AuditLogRepository auditLogRepository,
            RoleService roleService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.userSessionRepository = userSessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.roleService = roleService;
    }

    @Transactional(readOnly = true)
    public UserProfileView getProfile(RoleProfile profile, Long targetUserId, Long callerUserId) {
        if (!isAuthorizedToView(profile, targetUserId, callerUserId)) {
            throw new AccessDeniedException("Caller lacks permission to view this profile");
        }
        return buildProfile(targetUserId, callerUserId);
    }

    /** The base permission gate: full view permission, or self with the self-view permission. */
    public boolean isAuthorizedToView(RoleProfile profile, Long targetUserId, Long callerUserId) {
        boolean actingOnSelf = callerUserId.equals(targetUserId);
        return roleService.checkAccess(callerUserId, profile.viewPermission())
                || (actingOnSelf && profile.selfViewPermission() != null
                    && roleService.checkAccess(callerUserId, profile.selfViewPermission()));
    }

    /**
     * Assembles the aggregation with no permission check of its own — for a façade (e.g.
     * {@code StudentService}) that has already resolved an extra, role-specific scope (a teacher
     * sharing a group with the student) beyond the base gate above.
     */
    @Transactional(readOnly = true)
    public UserProfileView buildProfile(Long targetUserId, Long callerUserId) {
        User user = userRepository.findById(targetUserId).orElseThrow();
        boolean sensitiveFieldsAllowed = roleService.checkAccess(callerUserId, SENSITIVE_FIELD_PERMISSION);

        UserProfileView.PersonalInfo personalInfo = new UserProfileView.PersonalInfo(
                user.getPublicId(),
                user.getFullName(),
                user.getEmail(),
                user.getExternalRef(),
                user.getPhoneNumber(),
                user.getTimeZone(),
                user.getLocale(),
                user.getStatus(),
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.isMfaEnabled(),
                user.getMfaMethod(),
                user.getMfaEnrolledAt(),
                user.getEnrolmentPhotoPath(),
                user.getVoiceprintPath(),
                sensitiveFieldsAllowed ? user.getFailedLoginCount() : null,
                sensitiveFieldsAllowed ? user.getLockedUntil() : null);

        List<GroupMembership> memberships = groupMembershipRepository.findByUserId(targetUserId);
        List<UserProfileView.GroupMembershipView> groupMemberships = memberships.stream()
                .map(m -> {
                    StudentGroup group = studentGroupRepository.findById(m.getStudentGroupId()).orElse(null);
                    return new UserProfileView.GroupMembershipView(
                            m.getStudentGroupId(),
                            group == null ? null : group.getCode(),
                            group == null ? null : group.getName(),
                            m.isActive(), m.getJoinedAt(), m.getLeftAt());
                })
                .collect(Collectors.toList());

        Instant now = Instant.now();
        List<UserRole> grants = userRoleRepository.findByUserId(targetUserId);
        List<UserProfileView.RoleGrantView> roleGrants = grants.stream()
                .map(g -> {
                    Role role = roleRepository.findById(g.getRoleId()).orElse(null);
                    boolean active = g.getExpiresAt() == null || g.getExpiresAt().isAfter(now);
                    return new UserProfileView.RoleGrantView(
                            role == null ? null : role.getCode(), g.getGrantedAt(), g.getExpiresAt(), active);
                })
                .collect(Collectors.toList());

        Instant from = now.minus(RECENT_WINDOW_DAYS, ChronoUnit.DAYS);
        List<UserProfileView.LoginAttemptView> recentLoginAttempts = loginAttemptRepository
                .findByUserIdAndAttemptedAtBetweenOrderByAttemptedAtDesc(
                        targetUserId, from, now, PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(a -> new UserProfileView.LoginAttemptView(
                        a.getAttemptedAt(), a.getOutcome().name(), a.getIpAddress(), a.getGeoCountry()))
                .collect(Collectors.toList());

        List<UserProfileView.SessionView> recentSessions = userSessionRepository
                .findByUserIdAndIssuedAtBetweenOrderByIssuedAtDesc(
                        targetUserId, from, now, PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(s -> new UserProfileView.SessionView(
                        s.getIssuedAt(), s.getLastSeenAt(), s.getRevokedAt(), s.getRevokedReason()))
                .collect(Collectors.toList());

        List<ActivityEntry> activityTrail = mergedAuditTrail(targetUserId, from, now);

        return new UserProfileView(personalInfo, groupMemberships, roleGrants, recentLoginAttempts, recentSessions, activityTrail);
    }

    private List<ActivityEntry> mergedAuditTrail(Long targetUserId, Instant from, Instant to) {
        List<AuditLog> byActor = auditLogRepository
                .findByActorUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(targetUserId, from, to, PageRequest.of(0, RECENT_LIMIT))
                .getContent();
        List<AuditLog> onSubject = auditLogRepository
                .findByEntityTypeAndEntityIdAndOccurredAtBetweenOrderByOccurredAtDesc("User", targetUserId, from, to, PageRequest.of(0, RECENT_LIMIT))
                .getContent();

        return Stream.concat(byActor.stream(), onSubject.stream())
                .collect(Collectors.toMap(AuditLog::getId, a -> a, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparing(AuditLog::getOccurredAt).reversed())
                .limit(RECENT_LIMIT)
                .map(a -> new ActivityEntry(a.getOccurredAt(), "audit_logs", a.getAction().name(), describe(a)))
                .collect(Collectors.toList());
    }

    private static String describe(AuditLog a) {
        return a.getEntityType() + (a.getEntityId() == null ? "" : "#" + a.getEntityId())
                + (a.getReason() == null ? "" : " (" + a.getReason() + ")");
    }
}
