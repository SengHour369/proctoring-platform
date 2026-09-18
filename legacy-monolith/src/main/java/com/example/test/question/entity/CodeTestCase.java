package com.example.test.question.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.question.enums.TestCaseVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One input/expected-output pair a CODE question is graded against. Hangs off {@link Question},
 * not off a placement in an exam — same reasoning as {@link QuestionOption}: a test case is a
 * property of the bank item, so reusing the question in a second exam must not clone it.
 *
 * <p>{@code visibility} is one enum rather than two independent booleans (isSample/isHidden): a
 * closed vocabulary can't land in the contradictory state a pair of flags could.
 */
@Entity
@Table(
        name = "code_test_cases",
        uniqueConstraints = @UniqueConstraint(name = "uk_code_test_cases_order", columnNames = {"question_id", "sequence_no"}))
@Getter
@Setter
public class CodeTestCase extends BaseEntity {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "input_data", length = 4000)
    private String inputData;

    @Column(name = "expected_output", nullable = false, length = 4000)
    private String expectedOutput;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TestCaseVisibility visibility = TestCaseVisibility.HIDDEN;

    /** Null falls back to equal weighting across all of the question's test cases. */
    @Column(precision = 9, scale = 2)
    private BigDecimal points;
}
