package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.ExamSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSectionRepository extends JpaRepository<ExamSection, Long> {

    List<ExamSection> findByExamIdOrderBySequenceNoAsc(Long examId);

    long countByExamId(Long examId);

    Optional<ExamSection> findTopByExamIdOrderBySequenceNoDesc(Long examId);
}
