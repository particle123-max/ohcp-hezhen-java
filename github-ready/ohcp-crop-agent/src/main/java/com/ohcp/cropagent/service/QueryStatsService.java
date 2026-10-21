package com.ohcp.cropagent.service;

import com.ohcp.cropagent.domain.StatsSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class QueryStatsService {

    private final AtomicLong totalQueries = new AtomicLong();
    private final LongAdder totalRetrievalMs = new LongAdder();
    private final Map<String, LongAdder> nutrientCounts = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> recentPrompts = new ConcurrentLinkedDeque<>();

    public void record(String crop, String part, String nutrient, long elapsedMs) {
        totalQueries.incrementAndGet();
        totalRetrievalMs.add(Math.max(0, elapsedMs));
        nutrientCounts.computeIfAbsent(nutrient == null ? "未知" : nutrient, key -> new LongAdder()).increment();
        recentPrompts.addFirst(blankToDefault(crop, "作物") + " · " + blankToDefault(part, "症状"));
        while (recentPrompts.size() > 5) {
            recentPrompts.pollLast();
        }
    }

    public StatsSnapshot snapshot() {
        List<Map.Entry<String, LongAdder>> sorted = new ArrayList<>(nutrientCounts.entrySet());
        sorted.sort(Comparator.comparingLong((Map.Entry<String, LongAdder> item) -> item.getValue().sum()).reversed());

        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map.Entry<String, LongAdder> item : sorted) {
            counts.put(item.getKey(), item.getValue().sum());
        }

        long total = totalQueries.get();
        long average = total == 0 ? 0 : Math.round(totalRetrievalMs.sum() * 1.0 / total);
        return new StatsSnapshot(
                total,
                counts,
                sorted.isEmpty() ? "暂无" : sorted.get(0).getKey(),
                average,
                List.copyOf(recentPrompts)
        );
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
