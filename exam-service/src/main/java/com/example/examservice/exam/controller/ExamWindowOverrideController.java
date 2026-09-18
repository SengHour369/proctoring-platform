package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.ExamWindowOverrideResponse;
import com.example.examservice.exam.dto.GrantWindowOverrideRequest;
import com.example.examservice.exam.dto.ReasonRequest;
import com.example.examservice.exam.service.ExamWindowOverrideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExamWindowOverrideController {

    private final ExamWindowOverrideService examWindowOverrideService;

    @PostMapping("/api/exams/assignments/{assignmentId}/window-overrides")
    public ResponseEntity<ExamWindowOverrideResponse> grantOverride(
            @PathVariable Long assignmentId, @Valid @RequestBody GrantWindowOverrideRequest request) {
        ExamWindowOverrideResponse response =
                examWindowOverrideService.grantOverride(assignmentId, request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/exams/window-overrides/{overrideId}/revoke")
    public ExamWindowOverrideResponse revoke(@PathVariable Long overrideId,
                                              @Valid @RequestBody ReasonRequest request) {
        return examWindowOverrideService.revoke(overrideId, request.reason(), callerUserId());
    }

    private Long callerUserId() {
        return CallerUserIdResolver.require();
    }
}
