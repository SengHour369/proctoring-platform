package com.example.examservice.exam.dto;

import com.example.examservice.exam.entity.ExamSection;

import java.math.BigDecimal;

public record SectionResponse(
        Long id,
        Long examId,
        String title,
        String description,
        String instructions,
        int sequenceNo,
        Integer timeLimitMinutes,
        BigDecimal sectionPoints,
        boolean shuffleQuestions,
        Integer questionsToDraw,
        boolean lockOnExit
) {

    public static SectionResponse from(ExamSection section) {
        return new SectionResponse(
                section.getId(),
                section.getExamId(),
                section.getTitle(),
                section.getDescription(),
                section.getInstructions(),
                section.getSequenceNo(),
                section.getTimeLimitMinutes(),
                section.getSectionPoints(),
                section.isShuffleQuestions(),
                section.getQuestionsToDraw(),
                section.isLockOnExit());
    }
}
