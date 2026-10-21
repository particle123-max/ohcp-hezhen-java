package com.ohcp.cropagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohcp.cropagent.domain.KnowledgeBase;
import com.ohcp.cropagent.domain.KnowledgeEntry;
import com.ohcp.cropagent.domain.KnowledgeSummary;
import com.ohcp.cropagent.domain.SourceInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final ObjectMapper objectMapper;
    private final String externalFile;
    private volatile KnowledgeBase knowledgeBase;
    private volatile Map<String, SourceInfo> sourceIndex = Map.of();
    private volatile boolean loadedFromExternalFile;

    public KnowledgeBaseService(ObjectMapper objectMapper,
                                @Value("${app.knowledge.external-file:data/knowledge-base.json}") String externalFile) {
        this.objectMapper = objectMapper;
        this.externalFile = externalFile;
    }

    @PostConstruct
    public void initialize() {
        reload();
    }

    public synchronized KnowledgeSummary reload() {
        KnowledgeBase loaded = loadKnowledgeBase();
        validate(loaded);

        Map<String, SourceInfo> sources = new LinkedHashMap<>();
        for (SourceInfo source : loaded.sources()) {
            sources.put(source.id(), source);
        }

        this.knowledgeBase = loaded;
        this.sourceIndex = Map.copyOf(sources);
        log.info("本地知识库已加载：{} 条缺素知识，{} 个公开资料来源，来源={}",
                loaded.entries().size(), loaded.sources().size(), loadedFromExternalFile ? "外部JSON" : "JAR内置JSON");
        return summary();
    }

    public List<KnowledgeEntry> entries() {
        return knowledgeBase.entries();
    }

    public List<SourceInfo> sourcesFor(List<String> sourceIds) {
        if (sourceIds == null) {
            return List.of();
        }
        return sourceIds.stream()
                .map(sourceIndex::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public KnowledgeSummary summary() {
        KnowledgeBase current = knowledgeBase;
        return new KnowledgeSummary(
                current.version(),
                current.updatedAt(),
                current.entries().size(),
                current.sources().size(),
                current.entries().stream().map(KnowledgeEntry::nutrient).distinct().toList(),
                loadedFromExternalFile ? "外部 JSON 文件（支持热重载）" : "JAR 内置 JSON",
                loadedFromExternalFile
        );
    }

    private KnowledgeBase loadKnowledgeBase() {
        try {
            Path path = Path.of(externalFile).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                try (InputStream input = Files.newInputStream(path)) {
                    loadedFromExternalFile = true;
                    return objectMapper.readValue(input, KnowledgeBase.class);
                }
            }

            loadedFromExternalFile = false;
            ClassPathResource resource = new ClassPathResource("knowledge-base.json");
            try (InputStream input = resource.getInputStream()) {
                return objectMapper.readValue(input, KnowledgeBase.class);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("知识库加载失败，请检查 JSON 格式和文件路径：" + externalFile, exception);
        }
    }

    private void validate(KnowledgeBase loaded) {
        if (loaded == null || loaded.entries() == null || loaded.entries().isEmpty()) {
            throw new IllegalStateException("知识库至少需要一条缺素知识");
        }
        long distinctIds = loaded.entries().stream().map(KnowledgeEntry::id).distinct().count();
        if (distinctIds != loaded.entries().size()) {
            throw new IllegalStateException("知识库存在重复 id");
        }
        for (KnowledgeEntry entry : loaded.entries()) {
            if (entry.id() == null || entry.nutrient() == null || entry.symptoms() == null || entry.symptoms().isEmpty()) {
                throw new IllegalStateException("知识条目缺少 id、nutrient 或 symptoms：" + entry.id());
            }
        }
    }
}
