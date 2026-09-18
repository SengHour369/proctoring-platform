package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.exam.dto.CreateSectionRequest;
import com.example.examservice.exam.dto.ReorderSectionsRequest;
import com.example.examservice.exam.dto.SectionResponse;
import com.example.examservice.exam.dto.SetSectionTimeLimitRequest;
import com.example.examservice.exam.entity.Exam;
import com.example.examservice.exam.entity.ExamSection;
import com.example.examservice.exam.enums.ExamStatus;
import com.example.examservice.exam.repository.ExamRepository;
import com.example.examservice.exam.repository.ExamSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamSectionServiceImpl implements ExamSectionService {

    private final ExamRepository examRepository;
    private final ExamSectionRepository examSectionRepository;
    private final ExamVersioningService examVersioningService;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public SectionResponse createSection(Long examId, CreateSectionRequest request, Long callerUserId) {
        requirePermission(examId, callerUserId);

        Exam exam = loadExam(examId);
        if (request.title() == null || request.title().isBlank()) {
            throw new InvalidRequestException("title is required");
        }
        if (request.questionsToDraw() != null && request.questionsToDraw() < 1) {
            throw new InvalidRequestException("questionsToDraw must be >= 1");
        }
        if (request.timeLimitMinutes() != null && request.timeLimitMinutes() < 1) {
            throw new InvalidRequestException("timeLimitMinutes must be >= 1");
        }

        if (exam.getStatus() == ExamStatus.DRAFT) {
            int nextSeq = nextSequenceNo(examId, request.sequenceNo());
            ExamSection section = buildSection(examId, request, nextSeq);
            section = examSectionRepository.save(section);

            auditService.recordSuccess(AuditAction.CREATE, "ExamSection", section.getId(), callerUserId,
                    null, Map.of("examId", examId, "title", section.getTitle()), null);
            return SectionResponse.from(section);
        }

        requireReason(request.reason());
        ExamVersioningService.VersionCopy copy = examVersioningService.beginStructuralVersion(exam);
        int nextSeq = nextSequenceNoAmong(copy.sectionByOldId().values(), request.sequenceNo());
        ExamSection section = buildSection(copy.newExam().getId(), request, nextSeq);
        section = examSectionRepository.save(section);

        auditService.recordSuccess(AuditAction.CREATE, "ExamSection", section.getId(), callerUserId,
                null, Map.of("examId", copy.newExam().getId(), "title", section.getTitle()), request.reason());
        return SectionResponse.from(section);
    }

    @Override
    @Transactional
    public List<SectionResponse> reorderSections(Long examId, ReorderSectionsRequest request, Long callerUserId) {
        requirePermission(examId, callerUserId);

        Exam exam = loadExam(examId);
        List<ExamSection> currentSections = examSectionRepository.findByExamIdOrderBySequenceNoAsc(examId);
        Set<Long> currentIds = new HashSet<>();
        currentSections.forEach(s -> currentIds.add(s.getId()));
        Set<Long> requestedIds = new HashSet<>(request.orderedSectionIds());
        if (!currentIds.equals(requestedIds)) {
            throw new InvalidRequestException("orderedSectionIds must contain every section of the exam exactly once");
        }

        if (exam.getStatus() == ExamStatus.DRAFT) {
            Map<Long, ExamSection> byId = new java.util.HashMap<>();
            currentSections.forEach(s -> byId.put(s.getId(), s));
            List<ExamSection> updated = new ArrayList<>();
            int seq = 1;
            for (Long id : request.orderedSectionIds()) {
                ExamSection section = byId.get(id);
                section.setSequenceNo(seq++);
                updated.add(examSectionRepository.save(section));
            }
            auditService.recordSuccess(AuditAction.UPDATE, "ExamSection", examId, callerUserId,
                    null, null, "section_reorder");
            return updated.stream().map(SectionResponse::from).toList();
        }

        requireReason(request.reason());
        ExamVersioningService.VersionCopy copy = examVersioningService.beginStructuralVersion(exam);
        List<ExamSection> updated = new ArrayList<>();
        int seq = 1;
        for (Long oldId : request.orderedSectionIds()) {
            ExamSection newSection = copy.sectionByOldId().get(oldId);
            newSection.setSequenceNo(seq++);
            updated.add(examSectionRepository.save(newSection));
        }
        auditService.recordSuccess(AuditAction.UPDATE, "ExamSection", copy.newExam().getId(), callerUserId,
                null, null, "section_reorder");
        return updated.stream().map(SectionResponse::from).toList();
    }

    @Override
    @Transactional
    public SectionResponse setTimeLimit(Long sectionId, SetSectionTimeLimitRequest request, Long callerUserId) {
        ExamSection section = examSectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("ExamSection not found: " + sectionId));
        Exam exam = loadExam(section.getExamId());
        requirePermission(exam.getId(), callerUserId);

        Integer minutes = request.minutes();
        if (minutes != null && minutes < 1) {
            throw new InvalidRequestException("minutes must be >= 1");
        }
        validateSectionTimeBudget(exam, section.getId(), minutes);

        if (exam.getStatus() == ExamStatus.DRAFT) {
            section.setTimeLimitMinutes(minutes);
            section = examSectionRepository.save(section);
            auditService.recordSuccess(AuditAction.UPDATE, "ExamSection", section.getId(), callerUserId,
                    null, Map.of("timeLimitMinutes", String.valueOf(minutes)), null);
            return SectionResponse.from(section);
        }

        requireReason(request.reason());
        ExamVersioningService.VersionCopy copy = examVersioningService.beginStructuralVersion(exam);
        ExamSection newSection = copy.sectionByOldId().get(sectionId);
        if (newSection == null) {
            throw new EntityNotFoundException("ExamSection not found: " + sectionId);
        }
        newSection.setTimeLimitMinutes(minutes);
        newSection = examSectionRepository.save(newSection);
        auditService.recordSuccess(AuditAction.UPDATE, "ExamSection", newSection.getId(), callerUserId,
                null, Map.of("timeLimitMinutes", String.valueOf(minutes)), request.reason());
        return SectionResponse.from(newSection);
    }

    private void validateSectionTimeBudget(Exam exam, Long sectionId, Integer minutes) {
        if (minutes == null || exam.getDurationMinutes() == null) {
            return;
        }
        List<ExamSection> sections = examSectionRepository.findByExamIdOrderBySequenceNoAsc(exam.getId());
        int sumOthers = sections.stream()
                .filter(s -> !s.getId().equals(sectionId))
                .filter(s -> s.getTimeLimitMinutes() != null)
                .mapToInt(ExamSection::getTimeLimitMinutes)
                .sum();
        if (sumOthers + minutes > exam.getDurationMinutes()) {
            throw new InvalidRequestException("sum of section time limits would exceed exam duration");
        }
    }

    private ExamSection buildSection(Long examId, CreateSectionRequest request, int sequenceNo) {
        ExamSection section = new ExamSection();
        section.setExamId(examId);
        section.setTitle(request.title());
        section.setDescription(request.description());
        section.setInstructions(request.instructions());
        section.setSequenceNo(sequenceNo);
        section.setTimeLimitMinutes(request.timeLimitMinutes());
        section.setSectionPoints(request.sectionPoints());
        section.setShuffleQuestions(request.shuffleQuestions() != null && request.shuffleQuestions());
        section.setQuestionsToDraw(request.questionsToDraw());
        section.setLockOnExit(request.lockOnExit() != null && request.lockOnExit());
        return section;
    }

    private int nextSequenceNo(Long examId, Integer requested) {
        if (requested != null) {
            return requested;
        }
        return examSectionRepository.findTopByExamIdOrderBySequenceNoDesc(examId)
                .map(s -> s.getSequenceNo() + 1)
                .orElse(1);
    }

    private int nextSequenceNoAmong(java.util.Collection<ExamSection> sections, Integer requested) {
        if (requested != null) {
            return requested;
        }
        return sections.stream().mapToInt(ExamSection::getSequenceNo).max().orElse(0) + 1;
    }

    private Exam loadExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("reason is required for a structural change");
        }
    }

    private void requirePermission(Long examId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, Permissions.EXAM_UPDATE)) {
            auditService.recordDenied(AuditAction.UPDATE, "ExamSection", examId, callerUserId,
                    "missing " + Permissions.EXAM_UPDATE);
            throw new AccessDeniedException("missing permission: " + Permissions.EXAM_UPDATE);
        }
    }
}
