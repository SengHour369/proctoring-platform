package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.AccessCodeResponse;
import com.example.examservice.exam.dto.ValidateAccessCodeRequest;
import com.example.examservice.exam.dto.ValidateAccessCodeResponse;
import com.example.examservice.exam.service.ExamAccessCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExamAccessCodeController {

    private final ExamAccessCodeService examAccessCodeService;

    @PostMapping("/api/exams/assignments/{assignmentId}/access-code")
    public AccessCodeResponse generateCode(@PathVariable Long assignmentId) {
        return examAccessCodeService.generateCode(assignmentId, callerUserId());
    }

    @PostMapping("/api/exams/assignments/{assignmentId}/access-code/validate")
    public ValidateAccessCodeResponse validateCode(@PathVariable Long assignmentId,
                                                    @Valid @RequestBody ValidateAccessCodeRequest request) {
        return examAccessCodeService.validateCode(assignmentId, request.code());
    }

    private Long callerUserId() {
        return CallerUserIdResolver.require();
    }
}
