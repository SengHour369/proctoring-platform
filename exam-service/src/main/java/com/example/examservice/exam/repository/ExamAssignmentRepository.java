package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.ExamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment, Long> {

    Optional<ExamAssignment> findByExamIdAndCandidateUserId(Long examId, Long candidateUserId);
}
