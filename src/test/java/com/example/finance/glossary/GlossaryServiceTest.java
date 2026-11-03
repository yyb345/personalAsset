package com.example.finance.glossary;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GlossaryServiceTest {
    @Test void dictionaryAndFallback() throws Exception {
        var service = new GlossaryService(Optional.empty(),new ObjectMapper(),false);
        assertTrue(((Number) service.status().get("conceptCount")).intValue() >= 500);
        var response = service.highlight(List.of("LLM uses token and attention", "大语言模型使用注意力机制", "😀 <img src=x onerror=alert(1)> token"));
        assertEquals("local",response.engine());
        assertEquals(3,response.results().size());
        assertFalse(response.results().get(0).spans().isEmpty());
        assertFalse(response.results().get(1).spans().isEmpty());
        assertEquals("😀 <img src=x onerror=alert(1)> token",response.results().get(2).text());
    }
}
