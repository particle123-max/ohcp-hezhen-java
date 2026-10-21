package com.ohcp.cropagent.domain;

import java.util.List;

public record KnowledgeSummary(
        String version,
        String updatedAt,
        int entryCount,
        int sourceCount,
        List<String> nutrients,
        String storage,
        boolean externalFile
) {
}
