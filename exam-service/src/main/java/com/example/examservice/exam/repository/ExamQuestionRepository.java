package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    List<ExamQuestion> findByExamSectionIdOrderBySequenceNoAsc(Long examSectionId);

    List<ExamQuestion> findByExamSectionIdIn(List<Long> examSectionIds);

    Optional<ExamQuestion> findByExamSectionIdAndQuestionId(Long examSectionId, Long questionId);

    long countByExamSectionId(Long examSectionId);

    Optional<ExamQuestion> findTopByExamSectionIdOrderBySequenceNoDesc(Long examSectionId);
}
