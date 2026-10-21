package com.ohcp.cropagent.service;

import com.ohcp.cropagent.domain.KnowledgeEntry;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 面向 LangChain4j 智能体的本地知识库工具。
 * 即使不接大模型，核心 REST 链路也会直接调用同一检索器，确保离线可演示。
 */
@Component
public class KnowledgeBaseTools {

    private final KnowledgeBaseService knowledgeBaseService;
    private final TextMatcher textMatcher;

    public KnowledgeBaseTools(KnowledgeBaseService knowledgeBaseService, TextMatcher textMatcher) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.textMatcher = textMatcher;
    }

    @Tool("根据作物、症状部位和症状描述检索本地缺素症知识库，返回最多三条带证据的候选")
    public List<KnowledgeHit> searchDeficiency(
            @P("作物类型，例如：水稻、小麦、番茄") String crop,
            @P("症状部位，例如：下部老叶、新叶、叶缘") String part,
            @P("症状表现和环境描述") String symptoms) {
        String query = String.join(" ", safe(crop), safe(part), safe(symptoms));
        return textMatcher.search(query, knowledgeBaseService.entries()).stream()
                .limit(3)
                .map(result -> new KnowledgeHit(
                        result.entry().nutrient(),
                        result.entry().symbol(),
                        Math.round(result.score() * 10.0) / 10.0,
                        result.evidence(),
                        result.entry().description(),
                        result.entry().sourceIds()))
                .toList();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record KnowledgeHit(
            String nutrient,
            String symbol,
            double score,
            List<String> evidence,
            String description,
            List<String> sourceIds
    ) {
    }
}
