package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.ExamGroupAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamGroupAssignmentRepository extends JpaRepository<ExamGroupAssignment, Long> {

    Optional<ExamGroupAssignment> findByExamIdAndStudentGroupId(Long examId, Long studentGroupId);

    List<ExamGroupAssignment> findByStudentGroupIdAndCancelledAtIsNullAndAutoEnrollNewMembersTrue(Long studentGroupId);
}
