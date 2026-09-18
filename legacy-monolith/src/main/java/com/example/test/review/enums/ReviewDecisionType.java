package com.example.test.review.enums;

/** Action taken by a reviewer at one step of a case. */
public enum ReviewDecisionType {
    CLEAR,
    WARN_CANDIDATE,
    ADJUST_SCORE,
    INVALIDATE_ATTEMPT,
    GRANT_RETAKE,
    ESCALATE,
    REQUEST_MORE_INFO
}
