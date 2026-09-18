package com.example.examservice.question.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.question.dto.CategoryResponse;
import com.example.examservice.question.dto.CreateCategoryRequest;
import com.example.examservice.question.dto.MoveCategoryRequest;
import com.example.examservice.question.service.QuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/question-categories")
@RequiredArgsConstructor
public class QuestionCategoryController {

    private final QuestionBankService questionBankService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = questionBankService.createCategory(request, CallerUserIdResolver.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{categoryId}/move")
    public CategoryResponse moveCategory(@PathVariable Long categoryId, @RequestBody MoveCategoryRequest request) {
        return questionBankService.moveCategory(categoryId, request.newParentCategoryId(), CallerUserIdResolver.require());
    }
}
