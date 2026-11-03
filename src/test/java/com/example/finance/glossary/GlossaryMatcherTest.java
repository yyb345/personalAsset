package com.example.finance.glossary;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GlossaryMatcherTest {
    private GlossaryTerm term(String id, String en, String zh, String... aliases) {
        return new GlossaryTerm(id,en,zh,List.of(aliases),"Explanation", "解释", "AI",List.of());
    }
    @Test void boundariesChineseCaseAndUtf16() {
        var matcher = new GlossaryMatcher(List.of(term("token","token","词元"),term("llm","large language model","大语言模型","LLM")));
        String text = "😀 LLM使用token和词元，tokenizer不是token。";
        var spans = matcher.match(text,null);
        assertEquals(List.of("LLM","token","词元","token"),spans.stream().map(s -> text.substring(s.start(),s.end())).toList());
        assertEquals(3,spans.get(0).start());
        assertEquals(1,matcher.match("llm",Set.of("llm")).size());
        assertTrue(matcher.match("LLMs",null).isEmpty());
    }
    @Test void longestConceptWinsEvenWhenShortTermStartsEarlier() {
        var matcher = new GlossaryMatcher(List.of(term("attention","attention","注意力"),
            term("self","self-attention","自注意力")));
        var spans = matcher.match("self-attention and attention",null);
        assertEquals(List.of("self","attention"),spans.stream().map(GlossaryMatcher.Span::termId).toList());
        assertEquals(List.of("attention"),matcher.match("self-attention",Set.of("attention")).stream().map(GlossaryMatcher.Span::termId).toList());
    }
    @Test void preservesMarkupAsPlainTextAndMatchesPunctuationLiterally() {
        var matcher = new GlossaryMatcher(List.of(term("cpp","C++","C++语言")));
        String text = "<script>alert('C++')</script>";
        var spans = matcher.match(text,null);
        assertEquals("C++",text.substring(spans.get(0).start(),spans.get(0).end()));
        assertTrue(matcher.match("CCC",null).isEmpty());
    }
}
