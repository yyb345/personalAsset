package com.example.finance.learning;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebImportControllerTest {
    @Test void returnsExtractedTextAndStructuredFailures() throws Exception {
        var service = mock(WebImportService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new WebImportController(service)).build();
        when(service.extract("https://example.com")).thenReturn(new WebImportService.Article("Example", "A learning article", "https://example.com"));
        mvc.perform(post("/api/learning/web-import").contentType("application/json").content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Example")).andExpect(jsonPath("$.sourceUrl").value("https://example.com"));
        when(service.extract("http://localhost")).thenThrow(new WebImportService.ImportFailure(400, "仅支持公开网页"));
        mvc.perform(post("/api/learning/web-import").contentType("application/json").content("{\"url\":\"http://localhost\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("仅支持公开网页"));
    }
}
