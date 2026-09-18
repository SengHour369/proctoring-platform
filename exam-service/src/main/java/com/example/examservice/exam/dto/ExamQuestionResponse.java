package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.ExamQuestion;

import java.math.BigDecimal;

public record ExamQuestionResponse(
        Long id,
        Long examSectionId,
        Long questionId,
        int sequenceNo,
        BigDecimal points,
        BigDecimal negativePoints,
        boolean required,
        boolean shuffleOptions
) {

    public static ExamQuestionResponse from(ExamQuestion eq) {
        return new ExamQuestionResponse(
                eq.getId(),
                eq.getExamSectionId(),
                eq.getQuestionId(),
                eq.getSequenceNo(),
                eq.getPoints(),
                eq.getNegativePoints(),
                eq.isRequired(),
                eq.isShuffleOptions());
    }
}
