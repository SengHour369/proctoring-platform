package com.example.examservice.exam.controller;

import com.example.examservice.exam.service.ExamAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoint for the call {@code identity-service}'s real {@code GroupAutoEnrollPort}
 * implementation makes here after a new {@code GroupMembership} row is inserted — see
 * {@link ExamAssignmentService#autoEnrollNewMember}. Not part of the public API surface: intended
 * for service-to-service traffic only.
 */
@RestController
@RequiredArgsConstructor
public class GroupMembershipEventController {

    private final ExamAssignmentService examAssignmentService;

    @PostMapping("/api/internal/group-memberships/{studentGroupId}/candidates/{candidateUserId}/auto-enroll")
    public ResponseEntity<Void> autoEnroll(@PathVariable Long studentGroupId, @PathVariable Long candidateUserId) {
        examAssignmentService.autoEnrollNewMember(studentGroupId, candidateUserId);
        return ResponseEntity.noContent().build();
    }
}
