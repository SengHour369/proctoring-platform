package com.example.proctoringservice.proctoring.enums;

/** What a live human proctor did in response to something observed. */
public enum ProctorActionType {
    WARN,
    MESSAGE,
    FLAG,
    PAUSE,
    RESUME,
    TERMINATE,
    ESCALATE,
    NO_ACTION
}
