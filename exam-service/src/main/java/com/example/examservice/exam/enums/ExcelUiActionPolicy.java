package com.example.examservice.exam.enums;

/** How an exam's Excel runtime treats one candidate-initiated UI action (copy/paste, cut/drag-fill). */
public enum ExcelUiActionPolicy {
    ALLOW,
    BLOCK,
    LOG
}
