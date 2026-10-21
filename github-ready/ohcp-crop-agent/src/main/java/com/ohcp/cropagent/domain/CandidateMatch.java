package com.ohcp.cropagent.domain;

import java.util.List;

public record CandidateMatch(
        String knowledgeId,
        String nutrient,
        String symbol,
        int confidence,
        double score,
        String rationale,
        List<String> evidence,
        List<String> sourceIds
) {
}
