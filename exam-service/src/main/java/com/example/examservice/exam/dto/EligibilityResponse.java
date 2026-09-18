package com.example.examservice.exam.dto;

import java.math.BigDecimal;
import java.util.List;

public record EligibilityResponse(boolean eligible, List<FailedRule> failedRules) {

    public static EligibilityResponse ofEligible() {
        return new EligibilityResponse(true, List.of());
    }

    public static EligibilityResponse ofIneligible(List<FailedRule> failedRules) {
        return new EligibilityResponse(false, failedRules);
    }

    public record FailedRule(Long ruleId, Long requiredExamId, BigDecimal minScore, BigDecimal actualScore,
                              String reason) {
    }
}
