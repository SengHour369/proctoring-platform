package com.example.examservice.exam.repository;

import com.example.examservice.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Exam> findByPublicId(UUID publicId);
}
