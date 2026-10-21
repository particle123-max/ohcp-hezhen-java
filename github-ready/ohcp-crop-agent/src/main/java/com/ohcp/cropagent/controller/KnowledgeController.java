package com.ohcp.cropagent.controller;

import com.ohcp.cropagent.domain.KnowledgeSummary;
import com.ohcp.cropagent.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    public KnowledgeSummary summary() {
        return knowledgeBaseService.summary();
    }

    @PostMapping("/reload")
    public KnowledgeSummary reload() {
        return knowledgeBaseService.reload();
    }
}
