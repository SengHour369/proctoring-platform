package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.CreateExamRequest;
import com.example.examservice.exam.dto.ExamResponse;
import com.example.examservice.exam.dto.ReasonRequest;
import com.example.examservice.exam.dto.UpdateExamRequest;
import com.example.examservice.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody CreateExamRequest request) {
        ExamResponse response = examService.createExam(request, callerUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{examId}")
    public ExamResponse updateExam(@PathVariable Long examId, @RequestBody UpdateExamRequest request) {
        return examService.updateExam(examId, request, callerUserId());
    }

    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long examId, @Valid @RequestBody ReasonRequest request) {
        examService.deleteExam(examId, request.reason(), callerUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{examId}/publish")
    public ExamResponse publishExam(@PathVariable Long examId) {
        return examService.publishExam(examId, callerUserId());
    }

    @PostMapping("/{examId}/activate")
    public ExamResponse activateExam(@PathVariable Long examId) {
        return examService.activateExam(examId, callerUserId());
    }

    @PostMapping("/{examId}/close")
    public ExamResponse closeExam(@PathVariable Long examId, @Valid @RequestBody ReasonRequest request) {
        return examService.closeExam(examId, request.reason(), callerUserId());
    }

    @PostMapping("/{examId}/archive")
    public ExamResponse archiveExam(@PathVariable Long examId, @Valid @RequestBody ReasonRequest request) {
        return examService.archiveExam(examId, request.reason(), callerUserId());
    }

    private Long callerUserId() {
        return CallerUserIdResolver.require();
    }
}
