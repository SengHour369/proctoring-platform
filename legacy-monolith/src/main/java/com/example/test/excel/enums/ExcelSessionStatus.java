package com.example.test.excel.enums;

/** Lifecycle of one candidate's Excel runtime session for an attempt. */
public enum ExcelSessionStatus {
    PENDING,
    ACTIVE,
    SUBMITTED,
    CRASHED,
    RECOVERED
}
