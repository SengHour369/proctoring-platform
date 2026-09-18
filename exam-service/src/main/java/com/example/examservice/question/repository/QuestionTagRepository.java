package com.example.examservice.question.repository;

import com.example.examservice.question.entity.QuestionTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionTagRepository extends JpaRepository<QuestionTag, Long> {

    Optional<QuestionTag> findByName(String name);
}
