package com.example.examservice.exam.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.exam.dto.ExamQuestionResponse;
import com.example.examservice.exam.dto.PlaceQuestionRequest;
import com.example.examservice.exam.entity.Exam;
import com.example.examservice.exam.entity.ExamQuestion;
import com.example.examservice.exam.entity.ExamSection;
import com.example.examservice.exam.enums.ExamStatus;
import com.example.examservice.exam.repository.ExamQuestionRepository;
import com.example.examservice.exam.repository.ExamRepository;
import com.example.examservice.exam.repository.ExamSectionRepository;
import com.example.examservice.question.entity.Question;
import com.example.examservice.question.enums.QuestionStatus;
import com.example.examservice.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExamQuestionServiceImpl implements ExamQuestionService {

    private final ExamRepository examRepository;
    private final ExamSectionRepository examSectionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ExamVersioningService examVersioningService;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public ExamQuestionResponse placeQuestion(Long sectionId, PlaceQuestionRequest request, Long callerUserId) {
        requirePermission(sectionId, callerUserId);

        ExamSection section = examSectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("ExamSection not found: " + sectionId));
        Exam exam = examRepository.findById(section.getExamId())
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + section.getExamId()));
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + request.questionId()));

        if (question.getStatus() != QuestionStatus.ACTIVE) {
            throw new InvalidRequestException("question is not ACTIVE");
        }
        if (examQuestionRepository.findByExamSectionIdAndQuestionId(sectionId, request.questionId()).isPresent()) {
            throw new InvalidRequestException("question already placed in this section");
        }
        if (request.points() == null || request.points().signum() < 0) {
            throw new InvalidRequestException("points is required and must be >= 0");
        }
        if (request.negativePoints() != null && request.negativePoints().signum() < 0) {
            throw new InvalidRequestException("negativePoints must be >= 0");
        }

        if (exam.getStatus() == ExamStatus.DRAFT) {
            int seq = nextSequenceNo(sectionId, request.sequenceNo());
            ExamQuestion placement = buildPlacement(sectionId, request, seq);
            placement = examQuestionRepository.save(placement);
            auditService.recordSuccess(AuditAction.UPDATE, "ExamQuestion", placement.getId(), callerUserId,
                    null, Map.of("examSectionId", sectionId, "questionId", request.questionId()), null);
            return ExamQuestionResponse.from(placement);
        }

        requireReason(request.reason());
        ExamVersioningService.VersionCopy copy = examVersioningService.beginStructuralVersion(exam);
        ExamSection newSection = copy.sectionByOldId().get(sectionId);
        if (newSection == null) {
            throw new EntityNotFoundException("ExamSection not found: " + sectionId);
        }
        int seq = nextSequenceNo(newSection.getId(), request.sequenceNo());
        ExamQuestion placement = buildPlacement(newSection.getId(), request, seq);
        placement = examQuestionRepository.save(placement);
        auditService.recordSuccess(AuditAction.UPDATE, "ExamQuestion", placement.getId(), callerUserId,
                null, Map.of("examSectionId", newSection.getId(), "questionId", request.questionId()), request.reason());
        return ExamQuestionResponse.from(placement);
    }

    @Override
    @Transactional
    public void removeQuestion(Long placementId, String reason, Long callerUserId) {
        ExamQuestion placement = examQuestionRepository.findById(placementId)
                .orElseThrow(() -> new EntityNotFoundException("ExamQuestion not found: " + placementId));
        ExamSection section = examSectionRepository.findById(placement.getExamSectionId())
                .orElseThrow(() -> new EntityNotFoundException("ExamSection not found: " + placement.getExamSectionId()));
        Exam exam = examRepository.findById(section.getExamId())
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + section.getExamId()));
        requirePermission(exam.getId(), callerUserId);

        if (exam.getStatus() == ExamStatus.DRAFT) {
            examQuestionRepository.delete(placement);
            auditService.recordSuccess(AuditAction.DELETE, "ExamQuestion", placementId, callerUserId,
                    Map.of("examSectionId", placement.getExamSectionId(), "questionId", placement.getQuestionId()),
                    null, null);
            return;
        }

        requireReason(reason);
        ExamVersioningService.VersionCopy copy = examVersioningService.beginStructuralVersion(exam);
        ExamSection newSection = copy.sectionByOldId().get(section.getId());
        examQuestionRepository.findByExamSectionIdAndQuestionId(newSection.getId(), placement.getQuestionId())
                .ifPresent(examQuestionRepository::delete);
        auditService.recordSuccess(AuditAction.DELETE, "ExamQuestion", placementId, callerUserId,
                Map.of("examSectionId", placement.getExamSectionId(), "questionId", placement.getQuestionId()),
                null, reason);
    }

    private ExamQuestion buildPlacement(Long sectionId, PlaceQuestionRequest request, int sequenceNo) {
        ExamQuestion placement = new ExamQuestion();
        placement.setExamSectionId(sectionId);
        placement.setQuestionId(request.questionId());
        placement.setSequenceNo(sequenceNo);
        placement.setPoints(request.points() == null ? BigDecimal.ONE : request.points());
        placement.setNegativePoints(request.negativePoints());
        placement.setRequired(request.required() == null || request.required());
        placement.setShuffleOptions(request.shuffleOptions() != null && request.shuffleOptions());
        return placement;
    }

    private int nextSequenceNo(Long sectionId, Integer requested) {
        if (requested != null) {
            return requested;
        }
        return examQuestionRepository.findTopByExamSectionIdOrderBySequenceNoDesc(sectionId)
                .map(q -> q.getSequenceNo() + 1)
                .orElse(1);
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("reason is required for a structural change");
        }
    }

    private void requirePermission(Long entityId, Long callerUserId) {
        if (!roleService.checkAccess(callerUserId, Permissions.EXAM_UPDATE)) {
            auditService.recordDenied(AuditAction.UPDATE, "ExamQuestion", entityId, callerUserId,
                    "missing " + Permissions.EXAM_UPDATE);
            throw new AccessDeniedException("missing permission: " + Permissions.EXAM_UPDATE);
        }
    }
}
