package com.example.examservice.exam.service;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The one place that knows how to bump {@code Exam.version}. A structural edit past {@code DRAFT}
 * never mutates the live row — attempts in flight reference sections and questions by id, so
 * rewriting them in place would silently change what those attempts are on record as having been
 * given. Instead this copies the exam, its sections, and their question placements into a new row
 * tree, archives the old one, and hands the caller a mapping from old section id to new section so
 * a structural change (add a section, place a question, reorder, ...) can be applied to the copy.
 */
@Service
@RequiredArgsConstructor
public class ExamVersioningService {

    private final ExamRepository examRepository;
    private final ExamSectionRepository examSectionRepository;
    private final ExamQuestionRepository examQuestionRepository;

    /** Result of copying an exam for a structural edit: the new (unpersisted-changes-included)
     * exam row, and old-section-id -> new-section mapping for the caller to attach new/changed
     * placements to. */
    public record VersionCopy(Exam oldExam, Exam newExam, Map<Long, ExamSection> sectionByOldId) {
    }

    @Transactional
    public VersionCopy beginStructuralVersion(Exam exam) {
        String originalCode = exam.getCode();
        ExamStatus originalStatus = exam.getStatus();
        int originalVersion = exam.getVersion();

        // The old row keeps its unique `code` column until it is archived. Suffix it first so the
        // new version can be inserted with the original code in the same transaction — the old
        // row stays fully readable (past attempts reference it by id, not by code).
        exam.setCode(originalCode + "~v" + originalVersion);
        exam.setStatus(ExamStatus.ARCHIVED);
        examRepository.save(exam);
        examRepository.flush();

        Exam newExam = new Exam();
        newExam.setCode(originalCode);
        newExam.setTitle(exam.getTitle());
        newExam.setDescription(exam.getDescription());
        newExam.setInstructions(exam.getInstructions());
        newExam.setCreatedByUserId(exam.getCreatedByUserId());
        newExam.setStatus(originalStatus);
        newExam.setVersion(originalVersion + 1);
        newExam.setDurationMinutes(exam.getDurationMinutes());
        newExam.setOpensAt(exam.getOpensAt());
        newExam.setClosesAt(exam.getClosesAt());
        newExam.setMaxAttempts(exam.getMaxAttempts());
        newExam.setTotalPoints(exam.getTotalPoints());
        newExam.setPassingScore(exam.getPassingScore());
        newExam.setGradingMode(exam.getGradingMode());
        newExam.setShuffleSections(exam.isShuffleSections());
        newExam.setHoldResultsForReview(exam.isHoldResultsForReview());
        newExam.setShowResultImmediately(exam.isShowResultImmediately());
        copyProctoringPolicy(exam, newExam);
        copyExcelPolicy(exam, newExam);
        // The code unique-constraint would collide with the still-ARCHIVED-pending old row until
        // that row is saved in finalizeStructuralVersion within the same transaction; ordering
        // below (old row archived and flushed before the new row is inserted) keeps this safe.
        newExam = examRepository.save(newExam);

        List<ExamSection> oldSections = examSectionRepository.findByExamIdOrderBySequenceNoAsc(exam.getId());
        Map<Long, ExamSection> sectionByOldId = new HashMap<>();
        for (ExamSection oldSection : oldSections) {
            ExamSection newSection = new ExamSection();
            newSection.setExamId(newExam.getId());
            newSection.setTitle(oldSection.getTitle());
            newSection.setDescription(oldSection.getDescription());
            newSection.setInstructions(oldSection.getInstructions());
            newSection.setSequenceNo(oldSection.getSequenceNo());
            newSection.setTimeLimitMinutes(oldSection.getTimeLimitMinutes());
            newSection.setSectionPoints(oldSection.getSectionPoints());
            newSection.setShuffleQuestions(oldSection.isShuffleQuestions());
            newSection.setQuestionsToDraw(oldSection.getQuestionsToDraw());
            newSection.setLockOnExit(oldSection.isLockOnExit());
            newSection = examSectionRepository.save(newSection);
            sectionByOldId.put(oldSection.getId(), newSection);
        }

        if (!oldSections.isEmpty()) {
            List<Long> oldSectionIds = oldSections.stream().map(ExamSection::getId).toList();
            List<ExamQuestion> oldQuestions = examQuestionRepository.findByExamSectionIdIn(oldSectionIds);
            for (ExamQuestion oldQuestion : oldQuestions) {
                ExamSection newSection = sectionByOldId.get(oldQuestion.getExamSectionId());
                ExamQuestion newQuestion = new ExamQuestion();
                newQuestion.setExamSectionId(newSection.getId());
                newQuestion.setQuestionId(oldQuestion.getQuestionId());
                newQuestion.setSequenceNo(oldQuestion.getSequenceNo());
                newQuestion.setPoints(oldQuestion.getPoints());
                newQuestion.setNegativePoints(oldQuestion.getNegativePoints());
                newQuestion.setRequired(oldQuestion.isRequired());
                newQuestion.setShuffleOptions(oldQuestion.isShuffleOptions());
                examQuestionRepository.save(newQuestion);
            }
        }

        return new VersionCopy(exam, newExam, sectionByOldId);
    }

    private void copyProctoringPolicy(Exam source, Exam target) {
        var src = source.getProctoringPolicy();
        var dst = target.getProctoringPolicy();
        dst.setMode(src.getMode());
        dst.setRequireWebcam(src.isRequireWebcam());
        dst.setRequireScreenShare(src.isRequireScreenShare());
        dst.setRequireMicrophone(src.isRequireMicrophone());
        dst.setRequireIdentityCheck(src.isRequireIdentityCheck());
        dst.setRequireEnvironmentScan(src.isRequireEnvironmentScan());
        dst.setForceFullscreen(src.isForceFullscreen());
        dst.setMonitorScreen(src.isMonitorScreen());
        dst.setDetectTabSwitch(src.isDetectTabSwitch());
        dst.setDetectFace(src.isDetectFace());
        dst.setDetectMultipleFaces(src.isDetectMultipleFaces());
        dst.setDetectGaze(src.isDetectGaze());
        dst.setDetectObjects(src.isDetectObjects());
        dst.setDetectAudio(src.isDetectAudio());
        dst.setRetainAudioTranscript(src.isRetainAudioTranscript());
        dst.setBlockCopyPaste(src.isBlockCopyPaste());
        dst.setAllowedTabSwitches(src.getAllowedTabSwitches());
        dst.setAutoReviewRiskThreshold(src.getAutoReviewRiskThreshold());
        dst.setAutoTerminateRiskThreshold(src.getAutoTerminateRiskThreshold());
        dst.setEvidenceRetentionDays(src.getEvidenceRetentionDays());
        dst.setEvidenceCapturePreSeconds(src.getEvidenceCapturePreSeconds());
        dst.setEvidenceCapturePostSeconds(src.getEvidenceCapturePostSeconds());
    }

    private void copyExcelPolicy(Exam source, Exam target) {
        var src = source.getExcelPolicy();
        var dst = target.getExcelPolicy();
        dst.setRuntimeRequired(src.isRuntimeRequired());
        dst.setExternalAppBlocked(src.isExternalAppBlocked());
        dst.setMacrosAllowed(src.isMacrosAllowed());
        dst.setCopyPastePolicy(src.getCopyPastePolicy());
        dst.setCutDragFillPolicy(src.getCutDragFillPolicy());
        dst.setAutosaveIntervalSeconds(src.getAutosaveIntervalSeconds());
        dst.setSnapshotIntervalMinutes(src.getSnapshotIntervalMinutes());
        dst.setRecalcMode(src.getRecalcMode());
        dst.setIterativeCalcMaxIterations(src.getIterativeCalcMaxIterations());
        dst.setPrecisionAsDisplayed(src.isPrecisionAsDisplayed());
        dst.setVolatileFunctionsPinned(src.isVolatileFunctionsPinned());
    }
}
