package com.ohcp.cropagent.service;

import com.ohcp.cropagent.domain.DiagnosisRequest;
import com.ohcp.cropagent.domain.FertilizationAdvice;
import com.ohcp.cropagent.domain.KnowledgeEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AdviceFactory {

    public FertilizationAdvice create(DiagnosisRequest request, KnowledgeEntry entry) {
        String crop = blankToDefault(request.crop(), "作物");
        String stage = blankToDefault(request.growthStage(), "当前生育期");
        String soil = blankToDefault(request.soilType(), "当地土壤");

        List<String> agronomic = new ArrayList<>(safe(entry.agronomicMeasures()));
        agronomic.add("结合" + crop + stage + "长势进行田间复核，先选取小面积试验，确认无不良反应后再扩大。");

        List<String> organic = new ArrayList<>(safe(entry.organicPlan()));
        organic.add("有机肥应充分腐熟，并与化肥总量统筹，避免只增加投入而不减少无效养分。");

        List<String> chemical = new ArrayList<>(safe(entry.chemicalPlan()));
        chemical.add("具体肥料品种和用量应依据" + soil + "检测结果、目标产量及产品标签确定，本方案不替代测土配方。");

        Set<String> safety = new LinkedHashSet<>(safe(entry.safetyReminders()));
        safety.add("施肥后 3～5 天观察新叶、老叶和长势变化，异常时及时请当地农技人员复核。");
        safety.add("禁止将本识别结果作为唯一依据进行大面积、超量或盲目混配施肥。");

        String farmerAdvice = blankToDefault(entry.farmerSimpleAdvice(),
                "先小面积试验，结合土壤检测和当地农技指导科学施肥。")
                + " 当前为" + crop + stage + "，请优先保证水分和根系环境正常。";
        String professionalAdvice = blankToDefault(entry.professionalAdvice(),
                "结合土壤检测和目标产量制定配方，分次施用并跟踪作物响应。")
                + " 建议记录" + soil + "、施肥量和症状变化，为下一季测土配方积累数据。";

        return new FertilizationAdvice(
                "缺" + entry.nutrient() + "（" + entry.symbol() + "）",
                entry.description(),
                List.copyOf(agronomic),
                List.copyOf(organic),
                List.copyOf(chemical),
                List.copyOf(safety),
                farmerAdvice,
                professionalAdvice
        );
    }

    public String civicNote(String crop, KnowledgeEntry entry) {
        if ("N".equals(entry.symbol())) {
            return "一代代土壤肥料与农技推广工作者扎根田间，推动测土配方施肥和化肥减量增效。"
                    + "今天用 AI 读懂" + crop + "的缺素信号，也是在用技术守护粮食安全。";
        }
        if ("K".equals(entry.symbol()) || "P".equals(entry.symbol())) {
            return "科学施肥既要算清产量账，也要算好生态账。把" + crop + "的缺素诊断做准，"
                    + "就是让每一份肥料用在刀刃上，守住耕地质量和粮食安全。";
        }
        return "作物营养诊断连着产量，也连着绿色农业。用数据减少盲目施肥，"
                + "让" + crop + "吃得准、长得好，是青年把 AI 技术写进田间地头的具体行动。";
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }
}
