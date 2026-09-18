package com.example.examservice.question.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.question.dto.TagQuestionRequest;
import com.example.examservice.question.service.QuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuestionTagController {

    private final QuestionBankService questionBankService;

    @PostMapping("/api/questions/{questionId}/tags")
    public ResponseEntity<Void> tagQuestion(@PathVariable Long questionId, @Valid @RequestBody TagQuestionRequest request) {
        questionBankService.tagQuestion(questionId, request.tagName(), request.tagDescription(), CallerUserIdResolver.require());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/questions/{questionId}/tags/{tagId}")
    public ResponseEntity<Void> untagQuestion(@PathVariable Long questionId, @PathVariable Long tagId) {
        questionBankService.untagQuestion(questionId, tagId, CallerUserIdResolver.require());
        return ResponseEntity.noContent().build();
    }
}
