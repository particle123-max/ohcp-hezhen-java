package com.ohcp.cropagent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 可选的大模型润色层。缺素判断、候选排序和施肥事实均来自本地知识库；
 * 大模型仅可在显式开启时压缩思政提示语，不能生成新的施肥用量。
 */
@Service
public class LlmNarrator {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;
    private final boolean enabled;
    private final int maxOutputChars;

    public LlmNarrator(ObjectProvider<ChatClient.Builder> chatClientBuilder,
                       @Value("${app.llm.enabled:false}") boolean enabled,
                       @Value("${app.llm.max-output-chars:260}") int maxOutputChars) {
        this.chatClientBuilder = chatClientBuilder;
        this.enabled = enabled;
        this.maxOutputChars = maxOutputChars;
    }

    public Optional<String> polishCivicNote(String fallback, String crop, String deficiency) {
        if (!enabled) {
            return Optional.empty();
        }
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            return Optional.empty();
        }
        try {
            String prompt = """
                    你是农业科普助手。请只润色下面这段课程思政提示，使其自然、克制、贴合业务。
                    不得添加新的施肥用量、产量承诺或医学诊断，不得改变事实，输出不超过两句话。
                    作物：%s；缺素：%s。
                    原文：%s
                    """.formatted(crop, deficiency, fallback);
            String content = builder.build().prompt().user(prompt).call().content();
            if (content == null || content.isBlank()) {
                return Optional.empty();
            }
            String cleaned = content.strip().replaceAll("\\s+", " ");
            return Optional.of(cleaned.substring(0, Math.min(maxOutputChars, cleaned.length())));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
