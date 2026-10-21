package com.ohcp.cropagent;

import com.ohcp.cropagent.domain.DiagnosisRequest;
import com.ohcp.cropagent.domain.DiagnosisResponse;
import com.ohcp.cropagent.service.DiagnosisAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DiagnosisAgentServiceTest {

    @Autowired
    private DiagnosisAgentService service;

    @Test
    void identifiesNitrogenDeficiencyFromOldLeaves() {
        DiagnosisResponse response = service.diagnose(new DiagnosisRequest(
                "水稻", "下部老叶", "叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。",
                "分蘖期", "沙质土", "近期降雨较多"));

        assertThat(response.candidates()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(response.candidates().get(0).symbol()).isEqualTo("N");
        assertThat(response.candidates().get(0).confidence()).isGreaterThanOrEqualTo(70);
        assertThat(response.advice().agronomicMeasures()).isNotEmpty();
        assertThat(response.advice().chemicalPlan()).isNotEmpty();
        assertThat(response.advice().safetyReminders()).isNotEmpty();
        assertThat(response.civicNote()).contains("粮食安全");
        assertThat(response.sources()).isNotEmpty();
    }

    @Test
    void identifiesPotassiumDeficiencyFromLeafMargins() {
        DiagnosisResponse response = service.diagnose(new DiagnosisRequest(
                "小麦", "叶缘", "下部老叶叶缘发黄焦枯，向叶内扩展，茎秆软弱，植株倒伏。",
                "灌浆期", "沙质土", "强降雨"));

        assertThat(response.candidates().get(0).symbol()).isEqualTo("K");
        assertThat(response.risk().score()).isGreaterThanOrEqualTo(40);
    }

    @Test
    void identifiesIronDeficiencyFromYoungLeaves() {
        DiagnosisResponse response = service.diagnose(new DiagnosisRequest(
                "柑橘", "新叶", "新叶叶脉间失绿发黄，叶脉仍绿，嫩叶黄白，老叶相对正常。",
                "苗期", "石灰性土", "高 pH 且排水较差"));

        assertThat(response.candidates().get(0).symbol()).isEqualTo("Fe");
        assertThat(response.risk().factors()).isNotEmpty();
    }

    @Test
    void returnsLowConfidenceFallbackForUnclearDescription() {
        DiagnosisResponse response = service.diagnose(new DiagnosisRequest(
                "蔬菜", "全株", "长势一般，暂时没有明显症状。",
                "未知", "未知", "正常"));

        assertThat(response.candidates()).isNotEmpty();
        assertThat(response.candidates().get(0).confidence()).isLessThanOrEqualTo(50);
        assertThat(response.retrieval().strategy()).contains("RAG");
    }
}
