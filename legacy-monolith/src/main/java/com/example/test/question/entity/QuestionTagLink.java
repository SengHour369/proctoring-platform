package com.example.test.question.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Attachment of one tag to one bank question, resolving the many-to-many between QUESTIONS and
 * QUESTION_TAGS. Tag search runs from the tag side, so the index on {@code question_tag_id} is the
 * one that carries the load.
 */
@Entity
@Table(
        name = "question_tag_link",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_tag_link",
                columnNames = {"question_id", "question_tag_id"}),
        indexes = @Index(name = "ix_question_tag_link_tag", columnList = "question_tag_id"))
@Getter
@Setter
public class QuestionTagLink extends BaseEntity {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "question_tag_id", nullable = false)
    private Long questionTagId;
}
