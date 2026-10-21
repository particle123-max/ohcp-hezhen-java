package com.ohcp.cropagent.controller;

import com.ohcp.cropagent.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final boolean llmEnabled;

    public HealthController(KnowledgeBaseService knowledgeBaseService,
                            @Value("${app.llm.enabled:false}") boolean llmEnabled) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.llmEnabled = llmEnabled;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("time", OffsetDateTime.now().toString());
        result.put("knowledgeEntries", knowledgeBaseService.summary().entryCount());
        result.put("knowledgeVersion", knowledgeBaseService.summary().version());
        result.put("coreEngine", "LOCAL_RAG");
        result.put("springBoot", "3.5.16");
        result.put("springAi", "1.1.8");
        result.put("langchain4j", "1.22.0");
        result.put("llmPolishEnabled", llmEnabled);
        return result;
    }
}
