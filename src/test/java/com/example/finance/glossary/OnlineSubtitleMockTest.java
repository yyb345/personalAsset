package com.example.finance.glossary;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Offline regression using a snapshot downloaded from the user's production subtitle endpoint. */
class OnlineSubtitleMockTest {
    @Test void realTranscriptBatchesPreserveTextAndProduceValidExplanations() throws Exception {
        var mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode video;
        try (var input = new ClassPathResource("glossary/EsaUQNx59vA.json").getInputStream()) {
            video = mapper.readTree(input);
        }
        assertEquals("EsaUQNx59vA",video.path("videoId").asText());
        assertEquals(108,video.path("sentences").size());
        var service = new GlossaryService(Optional.empty(),mapper,false);
        var mvc = MockMvcBuilders.standaloneSetup(new GlossaryController(service)).build();
        var sentences = video.path("sentences");
        List<GlossaryService.HighlightResult> results = new ArrayList<>();
        Set<String> concepts = new TreeSet<>();
        Map<String,Integer> occurrences = new TreeMap<>();
        int matchedSentences=0, totalSpans=0;
        // Same batch size as TranscribeRead.vue: exercise the second, partial batch too.
        for (int offset=0; offset<sentences.size(); offset+=80) {
            List<String> texts = new ArrayList<>();
            for (int i=offset; i<Math.min(offset+80,sentences.size()); i++) texts.add(sentences.get(i).path("text").asText());
            var response = mvc.perform(post("/api/glossary/highlight").contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("texts",texts,"language","zh"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            var batch = mapper.readValue(response,GlossaryService.HighlightResponse.class);
            assertEquals("local",batch.engine());
            assertEquals(texts.size(),batch.results().size());
            for (int i=0; i<texts.size(); i++) {
                var result = batch.results().get(i);
                assertEquals(texts.get(i),result.text());
                int cursor=0;
                var reconstructed = new StringBuilder();
                for (var span : result.spans()) {
                    assertTrue(span.start()>=cursor && span.end()>span.start() && span.end()<=result.text().length());
                    assertNotNull(span.definitionEn());
                    assertFalse(span.definitionEn().isBlank());
                    assertNotNull(span.definitionZh());
                    assertFalse(span.definitionZh().isBlank());
                    reconstructed.append(result.text(),cursor,span.start()).append(result.text(),span.start(),span.end());
                    cursor=span.end();
                    concepts.add(span.termId());
                    occurrences.merge(span.termEn(),1,Integer::sum);
                }
                reconstructed.append(result.text().substring(cursor));
                assertEquals(result.text(),reconstructed.toString());
                if (!result.spans().isEmpty()) matchedSentences++;
                totalSpans+=result.spans().size();
                results.add(result);
            }
        }
        assertEquals(108,results.size());
        assertTrue(totalSpans>50,"Real transcript should exercise many term occurrences");
        assertTrue(results.get(0).spans().stream().anyMatch(s -> results.get(0).text().substring(s.start(),s.end()).equals("llm")),"Lowercase llm from production must match");
        assertTrue(results.stream().flatMap(r -> r.spans().stream()).anyMatch(s -> s.termEn().equalsIgnoreCase("token")));
        assertTrue(results.stream().anyMatch(r -> r.spans().stream().anyMatch(s -> r.text().substring(s.start(),s.end()).equals("tokens"))),"Real plural tokens must match");
        assertTrue(results.stream().anyMatch(r -> r.spans().stream().anyMatch(s -> r.text().substring(s.start(),s.end()).equals("large language models"))),"Real plural model names must match as a complete phrase");
        var report = Map.of("video",video,"highlight",new GlossaryService.HighlightResponse(results,"local"),
            "stats",Map.of("sentences",results.size(),"matchedSentences",matchedSentences,"spans",totalSpans,"concepts",concepts.size(),"occurrences",occurrences));
        Files.createDirectories(Path.of("target"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of("target/glossary-online-mock.json").toFile(),report);
        System.out.printf("ONLINE_SUBTITLE_MOCK sentences=%d matched=%d spans=%d concepts=%d%n",results.size(),matchedSentences,totalSpans,concepts.size());
    }
}
