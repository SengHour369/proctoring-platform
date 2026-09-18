package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.exam.dto.AddPrerequisiteRequest;
import com.example.examservice.exam.dto.EligibilityResponse;
import com.example.examservice.exam.dto.ExamPrerequisiteResponse;
import com.example.examservice.exam.entity.Exam;
import com.example.examservice.exam.entity.ExamPrerequisite;
import com.example.examservice.exam.port.ExamResultPort;
import com.example.examservice.exam.port.LmsIntegrationPort;
import com.example.examservice.exam.repository.ExamPrerequisiteRepository;
import com.example.examservice.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExamPrerequisiteServiceImpl implements ExamPrerequisiteService {

    private final ExamPrerequisiteRepository examPrerequisiteRepository;
    private final ExamRepository examRepository;
    private final ExamResultPort examResultPort;
    private final LmsIntegrationPort lmsIntegrationPort;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public ExamPrerequisiteResponse addRule(Long examId, AddPrerequisiteRequest request, Long callerUserId) {
        requirePermission(Permissions.PREREQUISITE_MANAGE, "ExamPrerequisite", null, callerUserId);

        loadExam(examId);

        boolean hasRequiredExam = request.requiredExamId() != null;
        boolean hasCourseReference = request.courseReference() != null && !request.courseReference().isBlank();

        if (hasRequiredExam && hasCourseReference) {
            throw new InvalidRequestException("requiredExamId and courseReference are mutually exclusive");
        }
        if (!hasRequiredExam && !hasCourseReference) {
            throw new InvalidRequestException("a rule must set either requiredExamId or courseReference");
        }
        if (hasRequiredExam) {
            if (request.requiredExamId().equals(examId)) {
                throw new InvalidRequestException("an exam cannot gate itself");
            }
            if (examRepository.findById(request.requiredExamId()).isEmpty()) {
                throw new EntityNotFoundException("Exam not found: " + request.requiredExamId());
            }
            if (request.minScore() == null) {
                throw new InvalidRequestException("minScore is required when requiredExamId is set");
            }
        }

        ExamPrerequisite rule = new ExamPrerequisite();
        rule.setExamId(examId);
        rule.setRequiredExamId(request.requiredExamId());
        rule.setMinScore(request.minScore());
        rule.setCourseReference(request.courseReference());
        rule.setDescription(request.description());
        rule.setActive(true);
        rule = examPrerequisiteRepository.save(rule);

        auditService.recordSuccess(AuditAction.CONFIG_CHANGE, "ExamPrerequisite", rule.getId(), callerUserId,
                null, Map.of("examId", examId, "requiredExamId", String.valueOf(request.requiredExamId()),
                        "courseReference", String.valueOf(request.courseReference())),
                "prerequisite_added");

        return ExamPrerequisiteResponse.from(rule);
    }

    @Override
    @Transactional
    public void retireRule(Long ruleId, String reason, Long callerUserId) {
        requirePermission(Permissions.PREREQUISITE_MANAGE, "ExamPrerequisite", ruleId, callerUserId);
        requireReason(reason);

        ExamPrerequisite rule = loadRule(ruleId);
        boolean beforeActive = rule.isActive();
        rule.setActive(false);
        examPrerequisiteRepository.save(rule);

        auditService.recordSuccess(AuditAction.CONFIG_CHANGE, "ExamPrerequisite", ruleId, callerUserId,
                Map.of("isActive", beforeActive), Map.of("isActive", false), reason);
    }

    @Override
    @Transactional(readOnly = true)
    public EligibilityResponse checkEligibility(Long examId, Long candidateUserId) {
        List<ExamPrerequisite> rules = examPrerequisiteRepository.findByExamIdAndActiveTrue(examId);
        List<EligibilityResponse.FailedRule> failed = new ArrayList<>();

        for (ExamPrerequisite rule : rules) {
            if (rule.getRequiredExamId() != null) {
                Optional<ExamResultPort.BestResult> best =
                        examResultPort.findBestFinalResult(candidateUserId, rule.getRequiredExamId());
                if (best.isEmpty()) {
                    failed.add(new EligibilityResponse.FailedRule(rule.getId(), rule.getRequiredExamId(),
                            rule.getMinScore(), null, "not_sat"));
                } else if (!best.get().passed()) {
                    failed.add(new EligibilityResponse.FailedRule(rule.getId(), rule.getRequiredExamId(),
                            rule.getMinScore(), best.get().finalScore(), "not_passed"));
                } else if (isBelowMinimum(best.get().finalScore(), rule.getMinScore())) {
                    failed.add(new EligibilityResponse.FailedRule(rule.getId(), rule.getRequiredExamId(),
                            rule.getMinScore(), best.get().finalScore(), "score_below_minimum"));
                }
            } else {
                Optional<Boolean> complete =
                        lmsIntegrationPort.isCourseworkComplete(candidateUserId, rule.getCourseReference());
                if (complete.isEmpty()) {
                    failed.add(new EligibilityResponse.FailedRule(rule.getId(), null, null, null, "lms_unavailable"));
                } else if (!complete.get()) {
                    failed.add(new EligibilityResponse.FailedRule(rule.getId(), null, null, null,
                            "coursework_incomplete"));
                }
            }
        }

        return failed.isEmpty() ? EligibilityResponse.ofEligible() : EligibilityResponse.ofIneligible(failed);
    }

    private boolean isBelowMinimum(BigDecimal actual, BigDecimal minScore) {
        return minScore != null && (actual == null || actual.compareTo(minScore) < 0);
    }

    private Exam loadExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
    }

    private ExamPrerequisite loadRule(Long ruleId) {
        return examPrerequisiteRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("ExamPrerequisite not found: " + ruleId));
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("reason is required");
        }
    }

    private void requirePermission(String permission, String entityType, Long entityId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, permission)) {
            auditService.recordDenied(AuditAction.CONFIG_CHANGE, entityType, entityId, callerUserId,
                    "missing " + permission);
            throw new AccessDeniedException("missing permission: " + permission);
        }
    }
}
