package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.ExamQuestionResponse;
import com.example.examservice.exam.dto.PlaceQuestionRequest;

public interface ExamQuestionService {

    ExamQuestionResponse placeQuestion(Long sectionId, PlaceQuestionRequest request, Long callerUserId);

    void removeQuestion(Long placementId, String reason, Long callerUserId);
}
