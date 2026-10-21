package com.ohcp.cropagent.domain;

import java.util.List;

public record KnowledgeBase(
        String version,
        String updatedAt,
        List<SourceInfo> sources,
        List<KnowledgeEntry> entries
) {
}
