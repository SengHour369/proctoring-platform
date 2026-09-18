package com.example.examservice.question.repository;

import com.example.examservice.question.entity.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, Long> {

    Optional<QuestionCategory> findByCode(String code);

    boolean existsByCode(String code);

    List<QuestionCategory> findByPathStartingWith(String pathPrefix);
}
