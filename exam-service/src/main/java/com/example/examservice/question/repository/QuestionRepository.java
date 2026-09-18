package com.example.examservice.question.repository;

import com.example.examservice.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    boolean existsByCode(String code);
}
