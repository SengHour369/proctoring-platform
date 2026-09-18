package com.example.examservice.exam.entity;

import com.example.examservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Ordered part of an exam ("Section A — Multiple choice"). Sections exist so an exam can mix
 * question styles, carry per-part time limits, and draw a random subset from a larger pool.
 */
@Entity
@Table(
        name = "exam_sections",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_sections_order", columnNames = {"exam_id", "sequence_no"}))
@Getter
@Setter
public class ExamSection extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 2000)
    private String instructions;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "section_points", precision = 9, scale = 2)
    private BigDecimal sectionPoints;

    @Column(name = "shuffle_questions", nullable = false)
    private boolean shuffleQuestions = false;

    /**
     * How many of the linked questions each candidate actually receives. Null delivers all of
     * them; a smaller number turns the section into a random draw from a pool.
     */
    @Column(name = "questions_to_draw")
    private Integer questionsToDraw;

    /** Once a candidate leaves this section they cannot navigate back into it. */
    @Column(name = "lock_on_exit", nullable = false)
    private boolean lockOnExit = false;
}
