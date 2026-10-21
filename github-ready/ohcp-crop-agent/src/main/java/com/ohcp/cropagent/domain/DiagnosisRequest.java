package com.ohcp.cropagent.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosisRequest(
        @NotBlank(message = "作物类型不能为空")
        String crop,

        @NotBlank(message = "症状部位不能为空")
        String part,

        @NotBlank(message = "请描述缺素症状")
        @Size(max = 600, message = "症状描述不能超过 600 字")
        String symptoms,

        String growthStage,
        String soilType,
        String environment
) {
    public String toQueryText() {
        return String.join(" ", safe(crop), safe(part), safe(symptoms), safe(growthStage), safe(soilType), safe(environment));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
