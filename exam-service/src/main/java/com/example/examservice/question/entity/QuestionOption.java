package com.example.examservice.question.entity;

import com.example.examservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One selectable choice of a choice-type question. Options hang off QUESTIONS, not off
 * EXAM_QUESTIONS: the choices are a property of the item itself, and reusing the item in a second
 * exam must not clone them.
 */
@Entity
@Table(
        name = "question_options",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_options_order", columnNames = {"question_id", "sequence_no"}))
@Getter
@Setter
public class QuestionOption extends BaseEntity {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** Display label — "A", "B", "1"… independent of storage order. */
    @Column(length = 8)
    private String label;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "media_path", length = 512)
    private String mediaPath;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    /** Partial credit for MULTIPLE_CHOICE scoring; null falls back to all-or-nothing. */
    @Column(name = "option_weight", precision = 9, scale = 2)
    private BigDecimal optionWeight;

    /** Shown to the candidate after grading when this option was picked. */
    @Column(length = 500)
    private String feedback;

    /** Right-hand value for MATCHING questions. */
    @Column(name = "match_key", length = 255)
    private String matchKey;
}
