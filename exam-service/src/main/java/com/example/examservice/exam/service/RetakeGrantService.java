package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.GrantRetakeRequest;
import com.example.examservice.exam.dto.RetakeGrantResponse;

public interface RetakeGrantService {

    RetakeGrantResponse grantRetake(Long assignmentId, GrantRetakeRequest request, Long callerUserId);

    /** Marks the grant consumed. Callers funding an attempt must do this in the same transaction
     * as the {@code ExamAttempt} insert so a grant can never fund two attempts. */
    void consume(Long grantId, Long attemptId);

    RetakeGrantResponse revoke(Long grantId, String reason, Long callerUserId);

    /**
     * {@code baseCap + sum(additionalAttempts)} across every unconsumed, unexpired, terminal grant
     * for the assignment. Never mutates {@code ExamAssignment.attemptsAllowed} or
     * {@code Exam.maxAttempts} — the raise only applies at this computation.
     */
    int computeEffectiveCap(Long examAssignmentId, int baseCap);
}
