package com.example.attemptservice.excel.enums;

/** Result of checking a submitted workbook's final hash against what the session actually produced. */
public enum ExcelIntegrityStatus {
    VALID,
    TAMPERED,
    INCONCLUSIVE
}
