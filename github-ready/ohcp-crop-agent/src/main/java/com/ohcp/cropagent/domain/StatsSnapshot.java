package com.ohcp.cropagent.domain;

import java.util.List;
import java.util.Map;

public record StatsSnapshot(
        long totalQueries,
        Map<String, Long> byNutrient,
        String topNutrient,
        long avgRetrievalMs,
        List<String> samplePrompts
) {
}


