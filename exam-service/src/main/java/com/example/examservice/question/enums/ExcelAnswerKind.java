package com.example.examservice.question.enums;

/**
 * What kind of check one {@code ExcelCellBinding} represents, and — reused on
 * {@code ExcelGradeResult} — what kind of check was actually applied to grade it. Kept as a
 * single shared enum rather than two parallel vocabularies: a binding's declared kind and the
 * grading actually performed must describe the same thing, except when a result falls back to
 * {@code MANUAL}.
 */
public enum ExcelAnswerKind {
    VALUE,
    FORMULA,
    RANGE,
    CHART,
    PIVOT_TABLE,
    CONDITIONAL_FORMATTING,
    NAMED_RANGE,
    MACRO_OUTPUT,
    MANUAL
}
