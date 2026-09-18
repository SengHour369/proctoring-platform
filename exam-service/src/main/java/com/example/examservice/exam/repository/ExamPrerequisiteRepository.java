package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.ExamPrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamPrerequisiteRepository extends JpaRepository<ExamPrerequisite, Long> {

    List<ExamPrerequisite> findByExamId(Long examId);

    List<ExamPrerequisite> findByExamIdAndActiveTrue(Long examId);
}
