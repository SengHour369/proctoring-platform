package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.ExamInvitationResponse;
import com.example.examservice.exam.dto.InvitationTokenRequest;
import com.example.examservice.exam.dto.SendInvitationRequest;
import com.example.examservice.exam.service.ExamInvitationService;
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
public class ExamInvitationController {

    private final ExamInvitationService examInvitationService;

    @PostMapping("/api/exams/assignments/{assignmentId}/invitations")
    public ResponseEntity<ExamInvitationResponse> sendInvitation(@PathVariable Long assignmentId,
                                                                   @Valid @RequestBody SendInvitationRequest request) {
        ExamInvitationResponse response =
                examInvitationService.sendInvitation(assignmentId, request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/exams/assignments/{assignmentId}/invitations/resend")
    public ResponseEntity<ExamInvitationResponse> resend(@PathVariable Long assignmentId,
                                                           @Valid @RequestBody SendInvitationRequest request) {
        ExamInvitationResponse response = examInvitationService.resend(assignmentId, request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/exams/invitations/{invitationId}/open")
    public ExamInvitationResponse recordOpen(@PathVariable Long invitationId,
                                              @Valid @RequestBody InvitationTokenRequest request) {
        return examInvitationService.recordOpen(invitationId, request.token());
    }

    @PostMapping("/api/exams/invitations/{invitationId}/accept")
    public ExamInvitationResponse recordAccept(@PathVariable Long invitationId,
                                                @Valid @RequestBody InvitationTokenRequest request) {
        return examInvitationService.recordAccept(invitationId, request.token());
    }

    private Long callerUserId() {
        return CallerUserIdResolver.require();
    }
}
