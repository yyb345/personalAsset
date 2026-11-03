package com.example.finance.glossary;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.StringReader;
import java.security.MessageDigest;
import java.util.*;

@Service
public class GlossaryService {
    private static final Logger log = LoggerFactory.getLogger(GlossaryService.class);
    private final ElasticsearchClient client;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final List<GlossaryTerm> terms;
    private final GlossaryMatcher matcher;
    private final String index;
    private volatile boolean ready;
    private volatile long retryAfter;
    public record HighlightResult(String text, List<GlossaryMatcher.Span> spans) {}
    public record HighlightResponse(List<HighlightResult> results, String engine) {}

    public GlossaryService(Optional<ElasticsearchClient> client, ObjectMapper mapper,
                           @Value("${elasticsearch.enabled:true}") boolean enabled) throws Exception {
        this.client = client.orElse(null);
        this.mapper = mapper;
        this.enabled = enabled;
        byte[] bytes;
        try (var stream = new ClassPathResource("glossary/terms.json").getInputStream()) {
            bytes = stream.readAllBytes();
        }
        terms = mapper.readValue(bytes, new TypeReference<List<GlossaryTerm>>() {});
        if (terms.isEmpty() || terms.stream().map(GlossaryTerm::id).distinct().count() != terms.size())
            throw new IllegalStateException("Glossary must contain unique concept ids");
        matcher = new GlossaryMatcher(terms);
        index = "ai_glossary_v1_" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0,16);
    }

    /** Content-addressed index prevents stale rules when the bundled dictionary changes. */
    private synchronized boolean ensureIndex() throws Exception {
        if (ready) return true;
        if (!enabled || client == null || System.currentTimeMillis() < retryAfter) return false;
        if (!client.indices().exists(e -> e.index(index)).value()) {
            try {
                client.indices().create(c -> c.index(index).withJson(new StringReader("""
                    {"settings":{"number_of_shards":1,"number_of_replicas":0},
                     "mappings":{"properties":{"text":{"type":"text","analyzer":"standard"},
                     "query":{"type":"percolator"}}}}
                    """)));
            } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
                if (!"resource_already_exists_exception".equals(e.error().type())) throw e;
            }
        }
        // Always upsert all rules on first use, repairing interrupted bulk initialization.
        for (int offset=0; offset<terms.size(); offset+=200) {
            List<BulkOperation> operations = new ArrayList<>();
            for (var term : terms.subList(offset, Math.min(offset+200, terms.size()))) {
                var should = term.names().stream().map(name -> Map.of("match_phrase", Map.of("text", name))).toList();
                var document = Map.of("query", Map.of("bool", Map.of("should", should, "minimum_should_match", 1)));
                operations.add(BulkOperation.of(b -> b.index(i -> i.index(index).id(term.id()).document(document))));
            }
            var response = client.bulk(b -> b.operations(operations));
            if (response.errors()) throw new IllegalStateException("Glossary bulk indexing failed: " + response.items().stream()
                .filter(i -> i.error()!=null).map(i -> i.error().reason()).findFirst().orElse("unknown"));
        }
        client.indices().refresh(r -> r.index(index));
        ready = true;
        log.info("Indexed {} glossary concepts in {}", terms.size(), index);
        return true;
    }

    public HighlightResponse highlight(List<String> texts) {
        List<Set<String>> candidates = null;
        try {
            if (ensureIndex()) {
                var documents = texts.stream().map(t -> Map.of("text", t)).toList();
                var body = Map.of("size", terms.size(), "_source", false,
                    "query", Map.of("constant_score", Map.of("filter", Map.of("percolate",
                        Map.of("field", "query", "documents", documents)))));
                String json = mapper.writeValueAsString(body);
                var response = client.search(s -> s.index(index).withJson(new StringReader(json)), JsonData.class);
                if (response.timedOut() || response.shards().failed().intValue() > 0)
                    throw new IllegalStateException("Incomplete glossary percolation");
                candidates = new ArrayList<>();
                for (int i=0; i<texts.size(); i++) candidates.add(new HashSet<>());
                for (var hit : response.hits().hits()) {
                    var slots = hit.fields().get("_percolator_document_slot");
                    if (slots != null) for (var slot : slots.to(List.class)) {
                        int position = ((Number) slot).intValue();
                        candidates.get(position).add(hit.id());
                    }
                }
            }
        } catch (Exception e) {
            candidates = null;
            ready = false;
            retryAfter = System.currentTimeMillis() + 60_000;
            log.warn("Glossary ES unavailable; using local exact matcher: {}", e.getMessage());
        }
        List<HighlightResult> results = new ArrayList<>();
        for (int i=0; i<texts.size(); i++) results.add(new HighlightResult(texts.get(i),
            matcher.match(texts.get(i), candidates == null ? null : candidates.get(i))));
        return new HighlightResponse(results, candidates == null ? "local" : "elasticsearch");
    }

    public Map<String, Object> status() {
        return Map.of("conceptCount", terms.size(), "index", index, "elasticsearchEnabled", enabled,
            "indexReady", ready, "engine", ready ? "elasticsearch" : "local");
    }
}
