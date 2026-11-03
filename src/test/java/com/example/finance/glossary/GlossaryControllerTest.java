package com.example.finance.glossary;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlossaryControllerTest {
    @Test void apiReturnsSpansAndRejectsInvalidRequests() throws Exception {
        var service = new GlossaryService(Optional.empty(),new ObjectMapper(),false);
        var mvc = MockMvcBuilders.standaloneSetup(new GlossaryController(service)).build();
        mvc.perform(post("/api/glossary/highlight").contentType("application/json")
            .content("{\"texts\":[\"LLM uses token\"],\"language\":\"zh\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.engine").value("local"))
            .andExpect(jsonPath("$.results[0].text").value("LLM uses token"))
            .andExpect(jsonPath("$.results[0].spans[0].start").value(0));
        for (String body : new String[]{"{}", "{\"texts\":[]}", "{\"texts\":[null]}", "{\"texts\":[\"x\"],\"language\":\"invalid\"}"}) {
            mvc.perform(post("/api/glossary/highlight").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
    }
}
