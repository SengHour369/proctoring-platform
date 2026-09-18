package com.example.test.precheck.enums;

/** Individual pre-flight check run before an attempt may start. */
public enum SystemCheckType {
    BROWSER_COMPATIBILITY,
    CAMERA,
    MICROPHONE,
    SPEAKER,
    NETWORK_BANDWIDTH,
    SCREEN_SHARE_PERMISSION,
    FULLSCREEN,
    ENVIRONMENT_SCAN,
    OS_COMPATIBILITY,
    SECOND_SCREEN,
    EXCEL_RUNTIME,
    FORMULA_ENGINE,
    WORKBOOK_LOAD
}
