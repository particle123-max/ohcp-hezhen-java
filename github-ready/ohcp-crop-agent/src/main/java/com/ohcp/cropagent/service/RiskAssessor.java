package com.ohcp.cropagent.service;

import com.ohcp.cropagent.domain.DiagnosisRequest;
import com.ohcp.cropagent.domain.KnowledgeEntry;
import com.ohcp.cropagent.domain.RiskAssessment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RiskAssessor {

    public RiskAssessment assess(DiagnosisRequest request, int confidence, KnowledgeEntry entry) {
        String context = String.join(" ",
                safe(request.crop()), safe(request.part()), safe(request.symptoms()),
                safe(request.growthStage()), safe(request.soilType()), safe(request.environment()))
                .toLowerCase(Locale.ROOT);

        int score = confidence < 55 ? 22 : 28;
        Set<String> factors = new LinkedHashSet<>();

        score += addIf(context, factors, List.of("沙", "砂"), 15, "沙质土保肥保水能力较弱，养分更易淋失");
        score += addIf(context, factors, List.of("涝", "积水", "渍水", "湿"), 16, "渍水会降低根系活力和养分吸收效率");
        score += addIf(context, factors, List.of("旱", "干旱", "缺水"), 12, "干旱会限制养分随质流到达根表");
        score += addIf(context, factors, List.of("低温", "寒冷", "倒春寒"), 10, "低温会抑制根系生长和养分吸收");
        score += addIf(context, factors, List.of("盐", "碱", "石灰", "高ph", "高 pH"), 12, "高盐或高 pH 可能降低部分微量元素有效性");
        score += addIf(context, factors, List.of("连作", "重茬"), 10, "连作地块容易出现养分失衡和根际微生态问题");
        score += addIf(context, factors, List.of("严重", "大面积", "枯死", "萎蔫"), 12, "症状已涉及大面积或严重表现，需尽快复核");
        score += addIf(context, factors, List.of("苗期", "开花", "花期", "灌浆", "孕穗"), 6, "当前处于关键生育期，缺素对产量影响更敏感");

        int matchedRiskFactors = 0;
        String soilAndEnvironment = (safe(request.soilType()) + " " + safe(request.environment())).toLowerCase(Locale.ROOT);
        for (String factor : safe(entry.riskFactors())) {
            String normalized = factor.toLowerCase(Locale.ROOT);
            if (soilAndEnvironment.contains(normalized) && matchedRiskFactors < 2) {
                factors.add("知识库命中风险条件：" + factor);
                score += 7;
                matchedRiskFactors++;
            }
        }

        score = Math.max(10, Math.min(95, score));
        String level = score < 40 ? "LOW" : score < 70 ? "MEDIUM" : "HIGH";
        String label = switch (level) {
            case "LOW" -> "低风险";
            case "MEDIUM" -> "中风险";
            default -> "高风险";
        };
        String suggestion = switch (level) {
            case "LOW" -> "可先按建议小面积调整，并在 3～5 天后观察新叶与长势变化。";
            case "MEDIUM" -> "建议先做土壤速测或请农技人员复核，施肥采用小面积试验后再扩大。";
            default -> "建议暂停大面积施肥，优先排查土壤、水分和根系问题，并请当地农技人员现场复核。";
        };

        if (factors.isEmpty()) {
            factors.add("暂未发现明显的叠加环境风险，但仍需结合土壤检测确认");
        }
        return new RiskAssessment(level, label, score, new ArrayList<>(factors), suggestion);
    }

    private int addIf(String context, Set<String> factors, List<String> keywords, int points, String description) {
        for (String keyword : keywords) {
            if (context.contains(keyword.toLowerCase(Locale.ROOT))) {
                factors.add(description);
                return points;
            }
        }
        return 0;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }
}
