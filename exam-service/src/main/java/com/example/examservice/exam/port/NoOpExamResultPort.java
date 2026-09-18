package com.example.examservice.exam.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default {@link ExamResultPort}: logs and reports no result, instead of calling
 * {@code result-service}. Returning empty makes exam-based prerequisite rules fail closed until a
 * real client is wired in.
 */
@Component
public class NoOpExamResultPort implements ExamResultPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpExamResultPort.class);

    @Override
    public Optional<BestResult> findBestFinalResult(Long candidateUserId, Long requiredExamId) {
        log.warn("[stub exam-result] would look up best FINAL result for candidate {} on exam {} in result-service",
                candidateUserId, requiredExamId);
        return Optional.empty();
    }
}
