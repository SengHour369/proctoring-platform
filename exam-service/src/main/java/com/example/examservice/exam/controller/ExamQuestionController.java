package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.ExamQuestionResponse;
import com.example.examservice.exam.dto.PlaceQuestionRequest;
import com.example.examservice.exam.dto.ReasonRequest;
import com.example.examservice.exam.service.ExamQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExamQuestionController {

    private final ExamQuestionService examQuestionService;

    @PostMapping("/api/exam-sections/{sectionId}/questions")
    public ResponseEntity<ExamQuestionResponse> placeQuestion(@PathVariable Long sectionId,
                                                               @Valid @RequestBody PlaceQuestionRequest request) {
        ExamQuestionResponse response = examQuestionService.placeQuestion(sectionId, request, CallerUserIdResolver.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api/exam-questions/{placementId}")
    public ResponseEntity<Void> removeQuestion(@PathVariable Long placementId,
                                                @RequestBody(required = false) ReasonRequest request) {
        String reason = request == null ? null : request.reason();
        examQuestionService.removeQuestion(placementId, reason, CallerUserIdResolver.require());
        return ResponseEntity.noContent().build();
    }
}
