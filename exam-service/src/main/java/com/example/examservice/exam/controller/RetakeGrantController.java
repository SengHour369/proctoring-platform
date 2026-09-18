package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.GrantRetakeRequest;
import com.example.examservice.exam.dto.ReasonRequest;
import com.example.examservice.exam.dto.RetakeGrantResponse;
import com.example.examservice.exam.service.RetakeGrantService;
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
public class RetakeGrantController {

    private final RetakeGrantService retakeGrantService;

    @PostMapping("/api/exams/assignments/{assignmentId}/retake-grants")
    public ResponseEntity<RetakeGrantResponse> grantRetake(@PathVariable Long assignmentId,
                                                            @Valid @RequestBody GrantRetakeRequest request) {
        RetakeGrantResponse response = retakeGrantService.grantRetake(assignmentId, request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/exams/retake-grants/{grantId}/revoke")
    public RetakeGrantResponse revoke(@PathVariable Long grantId, @Valid @RequestBody ReasonRequest request) {
        return retakeGrantService.revoke(grantId, request.reason(), callerUserId());
    }

    private Long callerUserId() {
        return CallerUserIdResolver.require();
    }
}
