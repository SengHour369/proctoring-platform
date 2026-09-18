package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.ExamWindowOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamWindowOverrideRepository extends JpaRepository<ExamWindowOverride, Long> {

    List<ExamWindowOverride> findByExamAssignmentIdOrderByGrantedAtDesc(Long examAssignmentId);
}
