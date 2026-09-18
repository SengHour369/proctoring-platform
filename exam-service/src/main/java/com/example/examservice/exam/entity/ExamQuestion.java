package com.example.examservice.exam.entity;

import com.example.examservice.common.entity.BaseEntity;
import com.example.examservice.question.entity.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Placement of a bank {@link Question} inside an {@link ExamSection}: the associative entity that
 * resolves the many-to-many between exams and questions. It is the unit answers and per-question
 * results point at, so the same bank question can appear in two exams worth different points
 * without either exam disturbing the other.
 */
@Entity
@Table(
        name = "exam_questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_exam_questions_question", columnNames = {"exam_section_id", "question_id"}),
                @UniqueConstraint(name = "uk_exam_questions_order", columnNames = {"exam_section_id", "sequence_no"})
        })
@Getter
@Setter
public class ExamQuestion extends BaseEntity {

    @Column(name = "exam_section_id", nullable = false)
    private Long examSectionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    /** Overrides the question's default weight for this exam only. */
    @Column(nullable = false, precision = 9, scale = 2)
    private BigDecimal points = BigDecimal.ONE;

    @Column(name = "negative_points", precision = 9, scale = 2)
    private BigDecimal negativePoints;

    @Column(nullable = false)
    private boolean required = true;

    @Column(name = "shuffle_options", nullable = false)
    private boolean shuffleOptions = false;
}
