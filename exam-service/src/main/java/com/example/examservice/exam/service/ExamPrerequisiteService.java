package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.AddPrerequisiteRequest;
import com.example.examservice.exam.dto.EligibilityResponse;
import com.example.examservice.exam.dto.ExamPrerequisiteResponse;

public interface ExamPrerequisiteService {

    ExamPrerequisiteResponse addRule(Long examId, AddPrerequisiteRequest request, Long callerUserId);

    void retireRule(Long ruleId, String reason, Long callerUserId);

    /** Read-only: no write, no audit. All active rules must pass (AND semantics), fail closed. */
    EligibilityResponse checkEligibility(Long examId, Long candidateUserId);
}
