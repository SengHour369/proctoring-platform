package com.example.examservice.question.repository;

import com.example.examservice.question.entity.QuestionTagLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionTagLinkRepository extends JpaRepository<QuestionTagLink, Long> {

    Optional<QuestionTagLink> findByQuestionIdAndQuestionTagId(Long questionId, Long questionTagId);
}
