package com.ohcp.cropagent.domain;

import java.util.List;

public record FertilizationAdvice(
        String deficiencyName,
        String introduction,
        List<String> agronomicMeasures,
        List<String> organicPlan,
        List<String> chemicalPlan,
        List<String> safetyReminders,
        String farmerSimpleAdvice,
        String professionalAdvice
) {
}
