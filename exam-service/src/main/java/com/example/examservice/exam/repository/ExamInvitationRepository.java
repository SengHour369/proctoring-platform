package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.ExamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamInvitationRepository extends JpaRepository<ExamInvitation, Long> {

    List<ExamInvitation> findByExamAssignmentIdOrderBySequenceNoDesc(Long examAssignmentId);

    Optional<ExamInvitation> findByTokenHash(String tokenHash);
}
