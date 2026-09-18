package com.example.examservice.question.enums;

/** Whether a SPREADSHEET question's macros may run, and under what constraint. */
public enum ExcelMacroPolicy {
    OFF,
    SANDBOXED,
    ALLOWED_WHITELIST
}
