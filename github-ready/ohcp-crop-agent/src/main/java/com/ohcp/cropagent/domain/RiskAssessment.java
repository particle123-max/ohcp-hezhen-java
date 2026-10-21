package com.ohcp.cropagent.domain;

import java.util.List;

public record RiskAssessment(
        String level,
        String label,
        int score,
        List<String> factors,
        String suggestion
) {
}
