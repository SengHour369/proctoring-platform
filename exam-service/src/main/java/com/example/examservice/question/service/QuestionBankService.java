package com.example.examservice.question.service;

import com.example.examservice.question.dto.CategoryResponse;
import com.example.examservice.question.dto.CreateCategoryRequest;

public interface QuestionBankService {

    CategoryResponse createCategory(CreateCategoryRequest request, Long callerUserId);

    CategoryResponse moveCategory(Long categoryId, Long newParentCategoryId, Long callerUserId);

    void tagQuestion(Long questionId, String tagName, String tagDescription, Long callerUserId);

    void untagQuestion(Long questionId, Long tagId, Long callerUserId);
}
