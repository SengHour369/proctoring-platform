package com.example.examservice.question.entity;

import com.example.examservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Subject taxonomy for the question bank — "Mathematics → Algebra → Quadratics". A tree rather than
 * a flat list because sections draw pools by subject area, and a draw at a parent node must reach
 * its children.
 */
@Entity
@Table(
        name = "question_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_categories_code", columnNames = "code"),
        indexes = @Index(name = "ix_question_categories_parent", columnList = "parent_category_id"))
@Getter
@Setter
public class QuestionCategory extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "parent_category_id")
    private Long parentCategoryId;

    /** Materialised path ("/MATH/ALGEBRA/"), so a subtree query is one LIKE rather than recursion. */
    @Column(name = "path", length = 512)
    private String path;

    @Column(name = "depth", nullable = false)
    private int depth = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
