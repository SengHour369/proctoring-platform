package com.example.identityservice.user.service;

import com.example.identityservice.auth.audit.entity.AuditLog;
import com.example.identityservice.auth.audit.repository.AuditLogRepository;
import com.example.identityservice.auth.config.AuthProperties;
import com.example.identityservice.auth.entity.LoginAttempt;
import com.example.identityservice.auth.entity.UserSession;
import com.example.identityservice.auth.exception.AccessDeniedException;
import com.example.identityservice.auth.repository.LoginAttemptRepository;
import com.example.identityservice.auth.repository.UserSessionRepository;
import com.example.identityservice.auth.service.RoleService;
import com.example.identityservice.user.dto.ActivityEntry;
import com.example.identityservice.user.dto.UserActivityPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * §2.5 — the "User Activity" read model. Not a table: it merges {@code audit_logs}, {@code
 * login_attempts} and {@code user_sessions} for one user, permission-masked and time-bounded, and
 * hands back one page at a time. (Exam-time {@code proctoring_events}/{@code proctor_actions} and
 * exam-lifecycle {@code exam_attempts}/{@code exam_assignments} live in other services' schemas —
 * see the module-level ports for how this module reaches across that boundary elsewhere; this
 * view surfaces only the sources identity-service itself owns.)
 */
@Service
public class UserActivityService {

    private static final int FETCH_PER_SOURCE = 200;

    private final RoleService roleService;
    private final AuditLogRepository auditLogRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserSessionRepository userSessionRepository;
    private final GroupScopeChecker groupScopeChecker;
    private final AuthProperties properties;

    public UserActivityService(
            RoleService roleService,
            AuditLogRepository auditLogRepository,
            LoginAttemptRepository loginAttemptRepository,
            UserSessionRepository userSessionRepository,
            GroupScopeChecker groupScopeChecker,
            AuthProperties properties) {
        this.roleService = roleService;
        this.auditLogRepository = auditLogRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.userSessionRepository = userSessionRepository;
        this.groupScopeChecker = groupScopeChecker;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public UserActivityPage getActivity(Long targetUserId, Instant from, Instant to, int page, int size, Long callerUserId) {
        Scope scope = resolveScope(targetUserId, callerUserId);

        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        if (Duration.between(from, to).toDays() > properties.getMaxActivityRangeDays()) {
            throw new IllegalArgumentException(
                    "Range exceeds the maximum of " + properties.getMaxActivityRangeDays() + " days");
        }

        boolean includeLoginAndSessionHistory = scope != Scope.CLASS
                || roleService.checkAccess(callerUserId, "security:view_login_history");

        // Deduplicated by (source, id): the same audit row can surface both as "by this actor" and
        // "about this entity" when a user acts on themselves (e.g. mfa enrolment).
        Map<String, ActivityEntry> merged = new LinkedHashMap<>();

        for (AuditLog log : auditLogRepository.findByActorUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
                targetUserId, from, to, PageRequest.of(0, FETCH_PER_SOURCE))) {
            merged.put("audit:" + log.getId(), toEntry(log));
        }
        for (AuditLog log : auditLogRepository.findByEntityTypeAndEntityIdAndOccurredAtBetweenOrderByOccurredAtDesc(
                "User", targetUserId, from, to, PageRequest.of(0, FETCH_PER_SOURCE))) {
            merged.put("audit:" + log.getId(), toEntry(log));
        }

        if (includeLoginAndSessionHistory) {
            for (LoginAttempt attempt : loginAttemptRepository.findByUserIdAndAttemptedAtBetweenOrderByAttemptedAtDesc(
                    targetUserId, from, to, PageRequest.of(0, FETCH_PER_SOURCE))) {
                merged.put("login:" + attempt.getId(), toEntry(attempt));
            }
            for (UserSession session : userSessionRepository.findByUserIdAndIssuedAtBetweenOrderByIssuedAtDesc(
                    targetUserId, from, to, PageRequest.of(0, FETCH_PER_SOURCE))) {
                merged.put("session:" + session.getId(), toEntry(session));
            }
        }

        List<ActivityEntry> sorted = new ArrayList<>(merged.values());
        sorted.sort(Comparator.comparing(ActivityEntry::occurredAt).reversed());

        int fromIndex = Math.min(page * size, sorted.size());
        int toIndex = Math.min(fromIndex + size, sorted.size());
        List<ActivityEntry> pageContent = sorted.subList(fromIndex, toIndex);
        boolean hasMore = toIndex < sorted.size();

        return new UserActivityPage(pageContent, page, size, hasMore);
    }

    private Scope resolveScope(Long targetUserId, Long callerUserId) {
        if (roleService.checkAccess(callerUserId, "activity:view")) {
            return Scope.FULL;
        }
        if (callerUserId.equals(targetUserId) && roleService.checkAccess(callerUserId, "activity:view_own")) {
            return Scope.SELF;
        }
        if (roleService.checkAccess(callerUserId, "activity:view_class")
                && groupScopeChecker.teacherSharesGroupWithStudent(callerUserId, targetUserId)) {
            return Scope.CLASS;
        }
        throw new AccessDeniedException("Caller lacks permission to view this user's activity");
    }

    private static ActivityEntry toEntry(AuditLog log) {
        String detail = log.getEntityType() + (log.getEntityId() == null ? "" : "#" + log.getEntityId())
                + (log.getReason() == null ? "" : " (" + log.getReason() + ")");
        return new ActivityEntry(log.getOccurredAt(), "audit_logs", log.getAction().name(), detail);
    }

    private static ActivityEntry toEntry(LoginAttempt attempt) {
        String detail = attempt.getIpAddress() == null ? "" : "from " + attempt.getIpAddress();
        return new ActivityEntry(attempt.getAttemptedAt(), "login_attempts", attempt.getOutcome().name(), detail);
    }

    private static ActivityEntry toEntry(UserSession session) {
        String action = session.getRevokedAt() != null ? "SESSION_REVOKED" : "SESSION_ISSUED";
        String detail = session.getRevokedReason() == null ? "" : session.getRevokedReason();
        Instant occurredAt = session.getRevokedAt() != null ? session.getRevokedAt() : session.getIssuedAt();
        return new ActivityEntry(occurredAt, "user_sessions", action, detail);
    }

    private enum Scope {
        FULL, SELF, CLASS
    }
}
