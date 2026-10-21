package com.ohcp.cropagent.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 无外部模型依赖的本地文本向量化器。
 * 将中文双字组映射为稀疏词频向量，用余弦相似度参与知识库重排。
 */
@Component
public class LocalTextVectorizer {

    public SparseVector vectorize(String text) {
        String normalized = normalize(text);
        Map<String, Integer> values = new HashMap<>();
        if (normalized.isEmpty()) {
            return new SparseVector(values);
        }
        if (normalized.length() == 1) {
            values.put(normalized, 1);
            return new SparseVector(values);
        }
        for (int index = 0; index < normalized.length() - 1; index++) {
            values.merge(normalized.substring(index, index + 2), 1, Integer::sum);
        }
        return new SparseVector(values);
    }

    public double cosine(SparseVector left, SparseVector right) {
        if (left.values().isEmpty() || right.values().isEmpty()) {
            return 0;
        }
        Map<String, Integer> smaller = left.values().size() <= right.values().size() ? left.values() : right.values();
        Map<String, Integer> larger = smaller == left.values() ? right.values() : left.values();

        double dot = 0;
        for (Map.Entry<String, Integer> entry : smaller.entrySet()) {
            Integer other = larger.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        if (dot == 0) {
            return 0;
        }
        return dot / (norm(left.values()) * norm(right.values()));
    }

    private double norm(Map<String, Integer> values) {
        double sum = 0;
        for (Integer value : values.values()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", "");
    }

    public record SparseVector(Map<String, Integer> values) {
    }
}
