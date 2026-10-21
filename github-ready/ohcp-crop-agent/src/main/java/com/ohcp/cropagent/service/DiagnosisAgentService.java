package com.ohcp.cropagent.service;

import com.ohcp.cropagent.domain.CandidateMatch;
import com.ohcp.cropagent.domain.DiagnosisRequest;
import com.ohcp.cropagent.domain.DiagnosisResponse;
import com.ohcp.cropagent.domain.FertilizationAdvice;
import com.ohcp.cropagent.domain.KnowledgeEntry;
import com.ohcp.cropagent.domain.RetrievalSummary;
import com.ohcp.cropagent.domain.RiskAssessment;
import com.ohcp.cropagent.domain.SourceInfo;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DiagnosisAgentService {

    private static final int TOP_K = 3;

    private final KnowledgeBaseService knowledgeBaseService;
    private final TextMatcher textMatcher;
    private final RiskAssessor riskAssessor;
    private final AdviceFactory adviceFactory;
    private final QueryStatsService queryStatsService;
    private final LlmNarrator llmNarrator;

    public DiagnosisAgentService(KnowledgeBaseService knowledgeBaseService,
                                 TextMatcher textMatcher,
                                 RiskAssessor riskAssessor,
                                 AdviceFactory adviceFactory,
                                 QueryStatsService queryStatsService,
                                 LlmNarrator llmNarrator) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.textMatcher = textMatcher;
        this.riskAssessor = riskAssessor;
        this.adviceFactory = adviceFactory;
        this.queryStatsService = queryStatsService;
        this.llmNarrator = llmNarrator;
    }

    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        long started = System.nanoTime();
        String query = request.toQueryText();
        List<TextMatcher.MatchResult> retrieved = textMatcher.search(query, knowledgeBaseService.entries());

        if (retrieved.isEmpty()) {
            retrieved = knowledgeBaseService.entries().stream()
                    .limit(TOP_K)
                    .map(entry -> new TextMatcher.MatchResult(entry, 0.1, List.of(), List.of()))
                    .toList();
        }

        List<TextMatcher.MatchResult> top = retrieved.stream().limit(TOP_K).toList();

        List<CandidateMatch> candidates = new ArrayList<>();
        Set<String> allMatchedTerms = new LinkedHashSet<>();

        for (TextMatcher.MatchResult result : top) {
            allMatchedTerms.addAll(result.matchedTerms());
            candidates.add(toCandidate(result));
        }

        TextMatcher.MatchResult best = top.get(0);
        KnowledgeEntry primary = best.entry();
        FertilizationAdvice advice = adviceFactory.create(request, primary);
        RiskAssessment risk = riskAssessor.assess(request, candidates.get(0).confidence(), primary);
        String civicNote = adviceFactory.civicNote(request.crop(), primary);
        String polished = llmNarrator.polishCivicNote(civicNote, request.crop(), primary.nutrient()).orElse(null);
        boolean llmEnhanced = polished != null;
        if (llmEnhanced) {
            civicNote = polished;
        }

        long elapsedMs = Math.max(1, (System.nanoTime() - started) / 1_000_000);
        queryStatsService.record(request.crop(), request.part(), primary.nutrient(), elapsedMs);

        List<SourceInfo> sources = knowledgeBaseService.sourcesFor(primary.sourceIds());
        RetrievalSummary retrieval = new RetrievalSummary(
                knowledgeBaseService.entries().size(),
                candidates.size(),
                allMatchedTerms.size(),
                elapsedMs,
                "本地 JSON 知识库 + 症状/部位/作物加权 + 中文双字组向量余弦重排（RAG）"
        );

        return new DiagnosisResponse(
                UUID.randomUUID().toString(),
                OffsetDateTime.now().toString(),
                llmEnhanced ? "LOCAL_RAG + OPTIONAL_LLM_POLISH" : "LOCAL_RAG",
                buildQuerySummary(request),
                List.copyOf(candidates),
                advice,
                risk,
                retrieval,
                sources,
                civicNote,
                llmEnhanced
        );
    }

    private CandidateMatch toCandidate(TextMatcher.MatchResult result) {
        int confidence;
        if (result.score() <= 0.2) {
            confidence = 28;
        } else {
            confidence = (int) Math.round(20 + 75 * (1 - Math.exp(-result.score() / 22.0)));
            confidence = Math.max(30, Math.min(95, confidence));
        }

        List<String> evidence = result.evidence().isEmpty()
                ? result.entry().symptoms().stream().limit(3).toList()
                : result.evidence().stream().limit(4).toList();
        String rationale;
        if (!result.evidence().isEmpty()) {
            rationale = "命中典型症状：" + String.join("、", result.evidence().stream().limit(3).toList());
        } else if (!result.matchedTerms().isEmpty()) {
            rationale = "匹配线索：" + String.join("、", result.matchedTerms().stream().limit(3).toList());
        } else {
            rationale = "直接症状证据不足，已作为低置信度候选提示进一步排查。";
        }

        return new CandidateMatch(
                result.entry().id(),
                result.entry().nutrient(),
                result.entry().symbol(),
                confidence,
                Math.round(result.score() * 10.0) / 10.0,
                rationale,
                evidence,
                result.entry().sourceIds()
        );
    }

    private String buildQuerySummary(DiagnosisRequest request) {
        List<String> pieces = new ArrayList<>();
        pieces.add(blankToDefault(request.crop(), "作物未填写"));
        pieces.add(blankToDefault(request.part(), "部位未填写"));
        if (request.growthStage() != null && !request.growthStage().isBlank()) {
            pieces.add(request.growthStage().trim());
        }
        if (request.soilType() != null && !request.soilType().isBlank()) {
            pieces.add(request.soilType().trim());
        }
        return String.join(" · ", pieces);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}


