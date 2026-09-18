package com.example.examservice.question.entity;

import com.example.examservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Free-form label on a bank question, orthogonal to the category tree — "past-paper-2024",
 * "needs-diagram", "high-discrimination". A table, not a comma-separated column, because authors
 * filter the bank by tag and a typo'd tag is worse than no tag.
 */
@Entity
@Table(
        name = "question_tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_tags_name", columnNames = "name"))
@Getter
@Setter
public class QuestionTag extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;
}
