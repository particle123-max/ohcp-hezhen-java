package com.ohcp.cropagent.domain;

import java.util.List;

public record DiagnosisResponse(
        String traceId,
        String generatedAt,
        String engine,
        String querySummary,
        List<CandidateMatch> candidates,
        FertilizationAdvice advice,
        RiskAssessment risk,
        RetrievalSummary retrieval,
        List<SourceInfo> sources,
        String civicNote,
        boolean llmEnhanced
) {
}
