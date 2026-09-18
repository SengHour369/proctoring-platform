package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.AssignToGroupRequest;
import com.example.examservice.exam.dto.AssignToStudentRequest;
import com.example.examservice.exam.dto.ExamAssignmentResponse;
import com.example.examservice.exam.dto.GroupAssignmentResponse;
import com.example.examservice.exam.dto.ReasonRequest;
import com.example.examservice.exam.service.ExamAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExamAssignmentController {

    private final ExamAssignmentService examAssignmentService;

    @PostMapping("/api/exams/{examId}/assignments")
    public ResponseEntity<ExamAssignmentResponse> assignToStudent(@PathVariable Long examId,
                                                                    @Valid @RequestBody AssignToStudentRequest request) {
        ExamAssignmentResponse response = examAssignmentService.assignToStudent(examId, request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/exams/{examId}/group-assignments")
    public ResponseEntity<GroupAssignmentResponse> assignToGroup(@PathVariable Long examId,
                                                                   @Valid @RequestBody AssignToGroupRequest request) {
        GroupAssignmentResponse response = examAssignmentService.assignToGroup(examId, request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/exams/{examId}/class-assignments")
    public ResponseEntity<GroupAssignmentResponse> assignToClass(@PathVariable Long examId,
                                                                   @Valid @RequestBody AssignToGroupRequest request) {
        GroupAssignmentResponse response = examAssignmentService.assignToClass(examId, request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/exams/assignments/{assignmentId}/cancel")
    public ExamAssignmentResponse cancelAssignment(@PathVariable Long assignmentId,
                                                    @Valid @RequestBody ReasonRequest request) {
        return examAssignmentService.cancelAssignment(assignmentId, request.reason(), callerUserId());
    }

    private Long callerUserId() {
        return CallerUserIdResolver.require();
    }
}
