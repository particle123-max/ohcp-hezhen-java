package com.ohcp.cropagent.domain;

import java.util.List;

public record KnowledgeEntry(
        String id,
        String nutrient,
        String symbol,
        List<String> aliases,
        List<String> crops,
        List<String> parts,
        List<String> symptoms,
        String description,
        List<String> agronomicMeasures,
        List<String> organicPlan,
        List<String> chemicalPlan,
        List<String> safetyReminders,
        List<String> riskFactors,
        List<String> tags,
        List<String> sourceIds,
        String farmerSimpleAdvice,
        String professionalAdvice
) {
    public String searchableText() {
        return String.join(" ",
                nutrient, symbol,
                String.join(" ", safe(aliases)),
                String.join(" ", safe(crops)),
                String.join(" ", safe(parts)),
                String.join(" ", safe(symptoms)),
                description,
                String.join(" ", safe(tags)),
                String.join(" ", safe(riskFactors)));
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }
}
