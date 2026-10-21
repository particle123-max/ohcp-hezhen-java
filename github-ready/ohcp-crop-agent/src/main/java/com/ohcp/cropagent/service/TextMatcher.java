package com.ohcp.cropagent.service;

import com.ohcp.cropagent.domain.KnowledgeEntry;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class TextMatcher {

    private final LocalTextVectorizer vectorizer;

    public TextMatcher(LocalTextVectorizer vectorizer) {
        this.vectorizer = vectorizer;
    }

    public List<MatchResult> search(String rawQuery, List<KnowledgeEntry> entries) {
        String query = normalize(rawQuery);
        Set<String> queryBigrams = bigrams(query);
        List<MatchResult> results = new ArrayList<>();

        for (KnowledgeEntry entry : entries) {
            double score = 0.0;
            Set<String> evidence = new LinkedHashSet<>();
            Set<String> matchedTerms = new LinkedHashSet<>();

            for (String symptom : safe(entry.symptoms())) {
                if (termMatches(query, queryBigrams, symptom)) {
                    double weight = Math.min(8.0, 3.5 + normalize(symptom).length() * 0.45);
                    score += weight;
                    evidence.add(symptom);
                    matchedTerms.add(symptom);
                }
            }

            for (String alias : safe(entry.aliases())) {
                if (termMatches(query, queryBigrams, alias)) {
                    score += 4.5;
                    matchedTerms.add(alias);
                }
            }

            for (String part : safe(entry.parts())) {
                if (contains(query, part)) {
                    score += 2.8;
                    matchedTerms.add(part);
                }
            }

            boolean cropMatched = false;
            for (String crop : safe(entry.crops())) {
                if (!"通用".equals(crop) && contains(query, crop)) {
                    score += 4.5;
                    cropMatched = true;
                    matchedTerms.add(crop);
                }
            }
            if (!cropMatched) {
                score -= 0.8;
            }

            for (String tag : safe(entry.tags())) {
                if (termMatches(query, queryBigrams, tag)) {
                    score += 2.0;
                    matchedTerms.add(tag);
                }
            }

            for (String factor : safe(entry.riskFactors())) {
                if (termMatches(query, queryBigrams, factor)) {
                    score += 1.2;
                    matchedTerms.add(factor);
                }
            }

            double similarity = vectorizer.cosine(vectorizer.vectorize(query), vectorizer.vectorize(entry.searchableText()));
            score += similarity * 5.0;

            if (score > 0) {
                results.add(new MatchResult(entry, score, List.copyOf(evidence), List.copyOf(matchedTerms)));
            }
        }

        results.sort((left, right) -> Double.compare(right.score(), left.score()));
        return results;
    }

    private boolean termMatches(String query, Set<String> queryBigrams, String rawTerm) {
        String term = normalize(rawTerm);
        if (term.length() < 2) {
            return false;
        }
        if (query.contains(term)) {
            return true;
        }
        if (term.length() < 4) {
            return false;
        }
        Set<String> termBigrams = bigrams(term);
        if (termBigrams.isEmpty()) {
            return false;
        }
        long hits = termBigrams.stream().filter(queryBigrams::contains).count();
        return hits >= 2 && hits * 1.0 / termBigrams.size() >= 0.7;
    }

    private boolean contains(String normalizedQuery, String rawTerm) {
        String term = normalize(rawTerm);
        return term.length() >= 2 && normalizedQuery.contains(term);
    }

    private Set<String> bigrams(String value) {
        if (value.length() < 2) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (int index = 0; index < value.length() - 1; index++) {
            result.add(value.substring(index, index + 2));
        }
        return result;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^\\p{IsHan}a-z0-9]+", "");
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    public record MatchResult(
            KnowledgeEntry entry,
            double score,
            List<String> evidence,
            List<String> matchedTerms
    ) {
    }
}

