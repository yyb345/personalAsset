package com.example.finance.glossary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedGlossaryTest {
    @Test void everyReviewedAliasResolvesToItsCanonicalConcept() throws Exception {
        var mapper = new ObjectMapper();
        List<GlossaryTerm> terms;
        try (var input = new ClassPathResource("glossary/terms.json").getInputStream()) {
            terms = mapper.readValue(input, new TypeReference<List<GlossaryTerm>>() {});
        }
        var reviewed = mapper.readValue(Files.readString(Path.of("scripts/glossary/advanced_terms.json")),
            new TypeReference<List<GlossaryTerm>>() {});
        var matcher = new GlossaryMatcher(terms);
        var ids = terms.stream().map(GlossaryTerm::id).collect(java.util.stream.Collectors.toSet());
        for (var term : reviewed) {
            assertTrue(ids.contains(term.id()), "Supplement missing from packaged dictionary: " + term.id());
            for (String alias : term.names()) {
                String text = "😀 " + alias + "。";
                var spans = matcher.match(text, null);
                assertTrue(spans.stream().anyMatch(s -> s.termId().equals(term.id()) && s.start()==3 && s.end()==text.length()-1),
                    "Missing/ambiguous alias: " + alias + " for " + term.id());
            }
        }
    }

    @Test void distinguishesTrainingStagesAndPreservesExactOffsets() throws Exception {
        var service = new GlossaryService(Optional.empty(), new ObjectMapper(), false);
        var texts = List.of("😀 post-training uses DPO, GRPO and RLVR.",
            "后训练包含直接偏好优化和可验证奖励强化学习。",
            "post-training quantization uses GPTQ; QLoRA is different.",
            "KV cache quantization and speculative decoding support inference-time scaling.",
            "上下文工程结合工具调用和模型上下文协议。",
            "We react to this; endpoints remain unchanged.");
        var response = service.highlight(texts);
        var expected = List.of(List.of("post-training","DPO","GRPO","RLVR"),
            List.of("后训练","直接偏好优化","可验证奖励强化学习"),
            List.of("post-training quantization","GPTQ","QLoRA"),
            List.of("KV cache quantization","speculative decoding","inference-time scaling"),
            List.of("上下文工程","工具调用","模型上下文协议"));
        for (int i=0; i<expected.size(); i++) {
            var result = response.results().get(i);
            assertEquals(texts.get(i),result.text());
            var surfaces = result.spans().stream().map(s -> result.text().substring(s.start(),s.end())).toList();
            assertTrue(surfaces.containsAll(expected.get(i)), "Missing phrase: " + surfaces);
            for (var span : result.spans()) {
                assertFalse(span.definitionEn().isBlank());
                assertFalse(span.definitionZh().isBlank());
            }
        }
        assertTrue(response.results().get(5).spans().stream().noneMatch(s ->
            s.termId().equals("ai-advanced-react-prompting") || s.termId().equals("ai-advanced-direct-preference-optimization")));
        assertEquals(3,response.results().get(0).spans().get(0).start());
    }

    @Test void agentsFoundationModelsAndReasoningAreCoveredWithoutConflatingMetrics() throws Exception {
        var service = new GlossaryService(Optional.empty(), new ObjectMapper(), false);
        var texts = List.of(
            "An agent harness loads Agent Skills and uses an Agent Card over A2A.",
            "The decoder-only model uses BPE, RMSNorm and SwiGLU with routed experts.",
            "CoT and MCTS differ from beam search and top-p sampling.",
            "智能体通过任务分解、工具模式定义和子智能体完成任务。",
            "稠密模型、分组查询注意力和多词元预测影响预训练。",
            "pass@1 pass^1 pass@k pass^k",
            "COTTON A2A2 MCTScan");
        var response = service.highlight(texts);
        var expected = List.of(
            List.of("agent harness", "Agent Skills", "Agent Card", "A2A"),
            List.of("decoder-only model", "BPE", "RMSNorm", "SwiGLU", "routed experts"),
            List.of("CoT", "MCTS", "beam search", "top-p sampling"),
            List.of("智能体", "任务分解", "工具模式定义", "子智能体"),
            List.of("稠密模型", "分组查询注意力", "多词元预测"));
        for (int i=0; i<expected.size(); i++) {
            var result = response.results().get(i);
            var surfaces = result.spans().stream().map(s -> result.text().substring(s.start(),s.end())).toList();
            assertTrue(surfaces.containsAll(expected.get(i)), "Missing category terms: " + surfaces);
        }
        assertEquals(List.of("ai-education-pass-at-k-passk", "ai-advanced-pass-to-k",
                "ai-education-pass-at-k-passk", "ai-advanced-pass-to-k"),
            response.results().get(5).spans().stream().map(GlossaryMatcher.Span::termId).toList());
        assertTrue(response.results().get(6).spans().isEmpty(), "Acronyms must respect word boundaries");
    }
}
