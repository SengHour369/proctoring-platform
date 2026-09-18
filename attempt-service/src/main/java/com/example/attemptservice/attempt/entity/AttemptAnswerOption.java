package com.example.attemptservice.attempt.entity;

import com.example.attemptservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * One option the candidate selected for one answer. Resolves the many-to-many between
 * ATTEMPT_ANSWERS and QUESTION_OPTIONS, needed because MULTIPLE_CHOICE, MATCHING and ORDERING all
 * select more than one.
 *
 * <p>{@code sequenceNo} is not redundant: for ORDERING and MATCHING questions the order the
 * candidate put the options in <em>is</em> the answer.
 */
@Entity
@Table(
        name = "attempt_answer_options",
        uniqueConstraints = @UniqueConstraint(name = "uk_attempt_answer_options",
                columnNames = {"attempt_answer_id", "question_option_id"}),
        indexes = @Index(name = "ix_attempt_answer_options_option", columnList = "question_option_id"))
@Getter
@Setter
public class AttemptAnswerOption extends BaseEntity {

    @Column(name = "attempt_answer_id", nullable = false)
    private Long attemptAnswerId;

    @Column(name = "question_option_id", nullable = false)
    private Long questionOptionId;

    /** Position the candidate placed this option in, for ordering and matching types. */
    @Column(name = "sequence_no")
    private Integer sequenceNo;
}
