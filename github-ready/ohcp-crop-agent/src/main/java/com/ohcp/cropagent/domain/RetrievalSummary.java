package com.ohcp.cropagent.domain;

public record RetrievalSummary(
        int knowledgeEntries,
        int candidatesReturned,
        int matchedTerms,
        long elapsedMs,
        String strategy
) {
}
