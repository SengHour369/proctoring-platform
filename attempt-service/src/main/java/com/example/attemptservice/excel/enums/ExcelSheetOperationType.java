package com.example.attemptservice.excel.enums;

/** A structural change to a sheet within an Excel session, as opposed to a cell-value edit. */
public enum ExcelSheetOperationType {
    INSERT,
    DELETE,
    RENAME,
    HIDE,
    UNHIDE,
    PROTECT,
    UNPROTECT
}
