package com.example.examservice.exam.port;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The call this module makes into {@code result-service} to evaluate an exam-based
 * {@code ExamPrerequisite} rule: the candidate's best {@code FINAL} result for the required exam.
 * Result data lives in result-service's own schema, not here — see {@link NoOpExamResultPort} for
 * the stub used until a real client is wired in. An unavailable result-service must be treated as
 * "no qualifying result", the same as {@link NoOpExamResultPort} returning empty, so a prerequisite
 * check fails closed rather than assuming eligibility.
 */
public interface ExamResultPort {

    Optional<BestResult> findBestFinalResult(Long candidateUserId, Long requiredExamId);

    record BestResult(boolean passed, BigDecimal finalScore) {
    }
}
