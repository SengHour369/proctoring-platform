package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.AddPrerequisiteRequest;
import com.example.examservice.exam.dto.EligibilityResponse;
import com.example.examservice.exam.dto.ExamPrerequisiteResponse;
import com.example.examservice.exam.dto.ReasonRequest;
import com.example.examservice.exam.service.ExamPrerequisiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExamPrerequisiteController {

    private final ExamPrerequisiteService examPrerequisiteService;

    @PostMapping("/api/exams/{examId}/prerequisites")
    public ResponseEntity<ExamPrerequisiteResponse> addRule(@PathVariable Long examId,
                                                             @RequestBody AddPrerequisiteRequest request) {
        ExamPrerequisiteResponse response = examPrerequisiteService.addRule(examId, request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/exams/prerequisites/{ruleId}/retire")
    public ResponseEntity<Void> retireRule(@PathVariable Long ruleId, @Valid @RequestBody ReasonRequest request) {
        examPrerequisiteService.retireRule(ruleId, request.reason(), callerUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/exams/{examId}/eligibility/{candidateUserId}")
    public EligibilityResponse checkEligibility(@PathVariable Long examId, @PathVariable Long candidateUserId) {
        return examPrerequisiteService.checkEligibility(examId, candidateUserId);
    }

    private Long callerUserId() {
        return CallerUserIdResolver.require();
    }
}
