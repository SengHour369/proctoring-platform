package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.AccessCodeResponse;
import com.example.examservice.exam.dto.ValidateAccessCodeResponse;

public interface ExamAccessCodeService {

    /** Returns the plaintext code exactly once. Replaces any existing code for the assignment. */
    AccessCodeResponse generateCode(Long assignmentId, Long callerUserId);

    ValidateAccessCodeResponse validateCode(Long assignmentId, String code);
}
