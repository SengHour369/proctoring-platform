package com.example.attemptservice.attempt.entity;

import com.example.attemptservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Outcome of running one CODE {@link AttemptAnswer} against one of its question's test cases.
 * Needed because {@link AttemptAnswer} is a single-row summary — correct for every other question
 * type — and once a question has several test cases, "which ones passed" has nowhere else to
 * live. Same reason {@link AttemptAnswerOption} exists as a child of {@link AttemptAnswer}.
 *
 * <p>A regrade overwrites the row in place, matching {@link AttemptAnswer}'s own convention —
 * unlike {@link AnswerRevision}, nothing here asks for a history of every regrade attempt.
 */
@Entity
@Table(
        name = "code_execution_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_code_execution_results",
                columnNames = {"attempt_answer_id", "code_test_case_id"}))
@Getter
@Setter
public class CodeExecutionResult extends BaseEntity {

    @Column(name = "attempt_answer_id", nullable = false)
    private Long attemptAnswerId;

    @Column(name = "code_test_case_id", nullable = false)
    private Long codeTestCaseId;

    @Column(nullable = false)
    private boolean passed = false;

    @Column(name = "actual_output", length = 4000)
    private String actualOutput;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "executed_at")
    private Instant executedAt;
}
