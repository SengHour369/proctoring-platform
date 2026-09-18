package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.exam.dto.CreateExamRequest;
import com.example.examservice.exam.dto.ExamResponse;
import com.example.examservice.exam.dto.ExcelPolicyDto;
import com.example.examservice.exam.dto.ProctoringPolicyDto;
import com.example.examservice.exam.dto.UpdateExamRequest;
import com.example.examservice.exam.entity.Exam;
import com.example.examservice.exam.entity.ExamQuestion;
import com.example.examservice.exam.entity.ExamSection;
import com.example.examservice.exam.enums.ExamStatus;
import com.example.examservice.exam.repository.ExamQuestionRepository;
import com.example.examservice.exam.repository.ExamRepository;
import com.example.examservice.exam.repository.ExamSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamSectionRepository examSectionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamVersioningService examVersioningService;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public ExamResponse createExam(CreateExamRequest request, Long callerUserId) {
        requirePermission(Permissions.EXAM_CREATE, "Exam", null, callerUserId);

        if (examRepository.existsByCode(request.code())) {
            throw new InvalidRequestException("code already in use");
        }
        validateWindow(request.opensAt(), request.closesAt());
        int maxAttempts = request.maxAttempts() == null ? 1 : request.maxAttempts();
        if (maxAttempts < 1) {
            throw new InvalidRequestException("maxAttempts must be >= 1");
        }
        if (request.passingScore() != null) {
            if (request.passingScore().signum() < 0) {
                throw new InvalidRequestException("passingScore must be >= 0");
            }
            if (request.totalPoints() != null && request.passingScore().compareTo(request.totalPoints()) > 0) {
                throw new InvalidRequestException("passingScore must be <= totalPoints");
            }
        }

        Exam exam = new Exam();
        exam.setCode(request.code());
        exam.setTitle(request.title());
        exam.setDescription(request.description());
        exam.setInstructions(request.instructions());
        exam.setCreatedByUserId(callerUserId);
        exam.setStatus(ExamStatus.DRAFT);
        exam.setVersion(1);
        exam.setDurationMinutes(request.durationMinutes());
        exam.setOpensAt(request.opensAt());
        exam.setClosesAt(request.closesAt());
        exam.setMaxAttempts(maxAttempts);
        exam.setTotalPoints(request.totalPoints());
        exam.setPassingScore(request.passingScore());
        if (request.gradingMode() != null) {
            exam.setGradingMode(request.gradingMode());
        }
        exam.setShuffleSections(bool(request.shuffleSections(), false));
        exam.setHoldResultsForReview(bool(request.holdResultsForReview(), false));
        exam.setShowResultImmediately(bool(request.showResultImmediately(), false));
        applyProctoringPolicy(exam, request.proctoringPolicy());
        applyExcelPolicy(exam, request.excelPolicy());

        exam = examRepository.save(exam);

        auditService.recordSuccess(AuditAction.CREATE, "Exam", exam.getId(), callerUserId,
                null, Map.of("status", exam.getStatus(), "code", exam.getCode(), "title", exam.getTitle()), null);

        return ExamResponse.from(exam);
    }

    @Override
    @Transactional
    public ExamResponse updateExam(Long examId, UpdateExamRequest request, Long callerUserId) {
        requirePermission(Permissions.EXAM_UPDATE, "Exam", examId, callerUserId);

        Exam exam = loadExam(examId);
        ExamStatus beforeStatus = exam.getStatus();

        boolean touchesProctoringPolicy = request.proctoringPolicy() != null;
        boolean structural = request.maxAttempts() != null
                || request.durationMinutes() != null
                || request.shuffleSections() != null
                || (touchesProctoringPolicy && exam.getStatus() != ExamStatus.DRAFT);

        if (structural && (request.reason() == null || request.reason().isBlank())) {
            throw new InvalidRequestException("reason is required for a structural change");
        }

        Exam target = exam;
        if (exam.getStatus() != ExamStatus.DRAFT && structural) {
            ExamVersioningService.VersionCopy copy = examVersioningService.beginStructuralVersion(exam);
            target = copy.newExam();
        }

        applyNonStructuralFields(target, request);
        if (request.maxAttempts() != null) {
            if (request.maxAttempts() < 1) {
                throw new InvalidRequestException("maxAttempts must be >= 1");
            }
            target.setMaxAttempts(request.maxAttempts());
        }
        if (request.durationMinutes() != null) {
            target.setDurationMinutes(request.durationMinutes());
        }
        if (request.shuffleSections() != null) {
            target.setShuffleSections(request.shuffleSections());
        }

        target = examRepository.save(target);

        auditService.recordSuccess(AuditAction.UPDATE, "Exam", examId, callerUserId,
                Map.of("status", beforeStatus), Map.of("status", target.getStatus(), "version", target.getVersion()),
                request.reason());

        return ExamResponse.from(target);
    }

    private void applyNonStructuralFields(Exam target, UpdateExamRequest request) {
        if (request.title() != null) {
            target.setTitle(request.title());
        }
        if (request.description() != null) {
            target.setDescription(request.description());
        }
        if (request.instructions() != null) {
            target.setInstructions(request.instructions());
        }
        if (request.showResultImmediately() != null) {
            target.setShowResultImmediately(request.showResultImmediately());
        }
        if (request.holdResultsForReview() != null) {
            target.setHoldResultsForReview(request.holdResultsForReview());
        }
        if (request.proctoringPolicy() != null) {
            applyProctoringPolicy(target, request.proctoringPolicy());
        }
        if (request.excelPolicy() != null) {
            applyExcelPolicy(target, request.excelPolicy());
        }
    }

    @Override
    @Transactional
    public void deleteExam(Long examId, String reason, Long callerUserId) {
        requirePermission(Permissions.EXAM_DELETE, "Exam", examId, callerUserId);
        requireReason(reason);

        Exam exam = loadExam(examId);
        ExamStatus beforeStatus = exam.getStatus();

        boolean hasNoAttempts = true; // ExamAttempt lives in attempt-service; nothing to check here.
        if (exam.getStatus() == ExamStatus.DRAFT && hasNoAttempts) {
            examRepository.delete(exam);
            auditService.recordSuccess(AuditAction.DELETE, "Exam", examId, callerUserId,
                    Map.of("status", beforeStatus), null, reason);
            return;
        }

        exam.setStatus(ExamStatus.ARCHIVED);
        examRepository.save(exam);
        auditService.recordSuccess(AuditAction.UPDATE, "Exam", examId, callerUserId,
                Map.of("status", beforeStatus), Map.of("status", ExamStatus.ARCHIVED), reason);
    }

    @Override
    @Transactional
    public ExamResponse publishExam(Long examId, Long callerUserId) {
        requirePermission(Permissions.EXAM_PUBLISH, "Exam", examId, callerUserId);

        Exam exam = loadExam(examId);
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new InvalidRequestException("exam must be DRAFT or SCHEDULED to publish");
        }

        List<ExamSection> sections = examSectionRepository.findByExamIdOrderBySequenceNoAsc(examId);
        if (sections.isEmpty()) {
            throw new InvalidRequestException("exam must have at least one section to publish");
        }

        List<Long> sectionIds = sections.stream().map(ExamSection::getId).toList();
        List<ExamQuestion> allQuestions = examQuestionRepository.findByExamSectionIdIn(sectionIds);
        if (allQuestions.isEmpty()) {
            throw new InvalidRequestException("exam must have at least one question to publish");
        }

        validateWindow(exam.getOpensAt(), exam.getClosesAt());
        if (exam.getMaxAttempts() < 1) {
            throw new InvalidRequestException("maxAttempts must be >= 1");
        }

        Integer examDuration = exam.getDurationMinutes();
        int sumSectionTimeLimits = 0;
        for (ExamSection section : sections) {
            long placedCount = allQuestions.stream().filter(q -> q.getExamSectionId().equals(section.getId())).count();
            if (section.getQuestionsToDraw() != null && section.getQuestionsToDraw() > placedCount) {
                throw new InvalidRequestException(
                        "section " + section.getId() + " questionsToDraw exceeds available questions");
            }
            if (section.getTimeLimitMinutes() != null) {
                sumSectionTimeLimits += section.getTimeLimitMinutes();
            }
            Set<Integer> seenSequenceNos = new java.util.HashSet<>();
            for (ExamQuestion q : allQuestions) {
                if (!q.getExamSectionId().equals(section.getId())) {
                    continue;
                }
                if (!seenSequenceNos.add(q.getSequenceNo())) {
                    throw new InvalidRequestException(
                            "duplicate sequenceNo in section " + section.getId());
                }
            }
        }
        if (examDuration != null && sumSectionTimeLimits > examDuration) {
            throw new InvalidRequestException("sum of section time limits exceeds exam duration");
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        exam = examRepository.save(exam);

        auditService.recordSuccess(AuditAction.PUBLISH, "Exam", examId, callerUserId,
                Map.of("status", ExamStatus.DRAFT), Map.of("status", ExamStatus.PUBLISHED), null);

        return ExamResponse.from(exam);
    }

    @Override
    @Transactional
    public ExamResponse activateExam(Long examId, Long callerUserId) {
        requirePermission(Permissions.EXAM_ACTIVATE, "Exam", examId, callerUserId);

        Exam exam = loadExam(examId);
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new InvalidRequestException("exam must be PUBLISHED to activate");
        }

        exam.setStatus(ExamStatus.ACTIVE);
        exam = examRepository.save(exam);

        auditService.recordSuccess(AuditAction.UPDATE, "Exam", examId, callerUserId,
                Map.of("status", ExamStatus.PUBLISHED), Map.of("status", ExamStatus.ACTIVE), null);

        return ExamResponse.from(exam);
    }

    @Override
    @Transactional
    public ExamResponse closeExam(Long examId, String reason, Long callerUserId) {
        requirePermission(Permissions.EXAM_CLOSE, "Exam", examId, callerUserId);
        requireReason(reason);

        Exam exam = loadExam(examId);
        if (exam.getStatus() != ExamStatus.ACTIVE) {
            throw new InvalidRequestException("exam must be ACTIVE to close");
        }

        exam.setStatus(ExamStatus.CLOSED);
        exam = examRepository.save(exam);

        auditService.recordSuccess(AuditAction.UPDATE, "Exam", examId, callerUserId,
                Map.of("status", ExamStatus.ACTIVE), Map.of("status", ExamStatus.CLOSED), reason);

        return ExamResponse.from(exam);
    }

    @Override
    @Transactional
    public ExamResponse archiveExam(Long examId, String reason, Long callerUserId) {
        requirePermission(Permissions.EXAM_ARCHIVE, "Exam", examId, callerUserId);
        requireReason(reason);

        Exam exam = loadExam(examId);
        if (!EnumSet.of(ExamStatus.CLOSED, ExamStatus.PUBLISHED).contains(exam.getStatus())) {
            throw new InvalidRequestException("exam must be CLOSED or PUBLISHED to archive");
        }

        ExamStatus before = exam.getStatus();
        exam.setStatus(ExamStatus.ARCHIVED);
        exam = examRepository.save(exam);

        auditService.recordSuccess(AuditAction.UPDATE, "Exam", examId, callerUserId,
                Map.of("status", before), Map.of("status", ExamStatus.ARCHIVED), reason);

        return ExamResponse.from(exam);
    }

    private void applyProctoringPolicy(Exam exam, ProctoringPolicyDto dto) {
        if (dto == null) {
            return;
        }
        var policy = exam.getProctoringPolicy();
        if (dto.mode() != null) policy.setMode(dto.mode());
        if (dto.requireWebcam() != null) policy.setRequireWebcam(dto.requireWebcam());
        if (dto.requireScreenShare() != null) policy.setRequireScreenShare(dto.requireScreenShare());
        if (dto.requireMicrophone() != null) policy.setRequireMicrophone(dto.requireMicrophone());
        if (dto.requireIdentityCheck() != null) policy.setRequireIdentityCheck(dto.requireIdentityCheck());
        if (dto.requireEnvironmentScan() != null) policy.setRequireEnvironmentScan(dto.requireEnvironmentScan());
        if (dto.forceFullscreen() != null) policy.setForceFullscreen(dto.forceFullscreen());
        if (dto.monitorScreen() != null) policy.setMonitorScreen(dto.monitorScreen());
        if (dto.detectTabSwitch() != null) policy.setDetectTabSwitch(dto.detectTabSwitch());
        if (dto.detectFace() != null) policy.setDetectFace(dto.detectFace());
        if (dto.detectMultipleFaces() != null) policy.setDetectMultipleFaces(dto.detectMultipleFaces());
        if (dto.detectGaze() != null) policy.setDetectGaze(dto.detectGaze());
        if (dto.detectObjects() != null) policy.setDetectObjects(dto.detectObjects());
        if (dto.detectAudio() != null) policy.setDetectAudio(dto.detectAudio());
        if (dto.retainAudioTranscript() != null) policy.setRetainAudioTranscript(dto.retainAudioTranscript());
        if (dto.blockCopyPaste() != null) policy.setBlockCopyPaste(dto.blockCopyPaste());
        if (dto.allowedTabSwitches() != null) policy.setAllowedTabSwitches(dto.allowedTabSwitches());
        if (dto.autoReviewRiskThreshold() != null) policy.setAutoReviewRiskThreshold(dto.autoReviewRiskThreshold());
        if (dto.autoTerminateRiskThreshold() != null) policy.setAutoTerminateRiskThreshold(dto.autoTerminateRiskThreshold());
        if (dto.evidenceRetentionDays() != null) policy.setEvidenceRetentionDays(dto.evidenceRetentionDays());
        if (dto.evidenceCapturePreSeconds() != null) policy.setEvidenceCapturePreSeconds(dto.evidenceCapturePreSeconds());
        if (dto.evidenceCapturePostSeconds() != null) policy.setEvidenceCapturePostSeconds(dto.evidenceCapturePostSeconds());
    }

    private void applyExcelPolicy(Exam exam, ExcelPolicyDto dto) {
        if (dto == null) {
            return;
        }
        var policy = exam.getExcelPolicy();
        if (dto.runtimeRequired() != null) policy.setRuntimeRequired(dto.runtimeRequired());
        if (dto.externalAppBlocked() != null) policy.setExternalAppBlocked(dto.externalAppBlocked());
        if (dto.macrosAllowed() != null) policy.setMacrosAllowed(dto.macrosAllowed());
        if (dto.copyPastePolicy() != null) policy.setCopyPastePolicy(dto.copyPastePolicy());
        if (dto.cutDragFillPolicy() != null) policy.setCutDragFillPolicy(dto.cutDragFillPolicy());
        if (dto.autosaveIntervalSeconds() != null) policy.setAutosaveIntervalSeconds(dto.autosaveIntervalSeconds());
        if (dto.snapshotIntervalMinutes() != null) policy.setSnapshotIntervalMinutes(dto.snapshotIntervalMinutes());
        if (dto.recalcMode() != null) policy.setRecalcMode(dto.recalcMode());
        if (dto.iterativeCalcMaxIterations() != null) policy.setIterativeCalcMaxIterations(dto.iterativeCalcMaxIterations());
        if (dto.precisionAsDisplayed() != null) policy.setPrecisionAsDisplayed(dto.precisionAsDisplayed());
        if (dto.volatileFunctionsPinned() != null) policy.setVolatileFunctionsPinned(dto.volatileFunctionsPinned());
    }

    private void validateWindow(java.time.Instant opensAt, java.time.Instant closesAt) {
        if (opensAt != null && closesAt != null && !closesAt.isAfter(opensAt)) {
            throw new InvalidRequestException("closesAt must be after opensAt");
        }
    }

    private Exam loadExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("reason is required");
        }
    }

    private void requirePermission(String permission, String entityType, Long entityId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, permission)) {
            AuditAction deniedAction = switch (permission) {
                case Permissions.EXAM_CREATE -> AuditAction.CREATE;
                case Permissions.EXAM_DELETE -> AuditAction.DELETE;
                case Permissions.EXAM_PUBLISH -> AuditAction.PUBLISH;
                default -> AuditAction.UPDATE;
            };
            auditService.recordDenied(deniedAction, entityType, entityId, callerUserId,
                    "missing " + permission);
            throw new AccessDeniedException("missing permission: " + permission);
        }
    }

    private boolean bool(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }
}
