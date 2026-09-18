package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.RetakeGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetakeGrantRepository extends JpaRepository<RetakeGrant, Long> {

    List<RetakeGrant> findByExamAssignmentId(Long examAssignmentId);
}
