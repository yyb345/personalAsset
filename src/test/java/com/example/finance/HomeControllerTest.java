package com.example.finance;

import com.example.finance.seo.SiteProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HomeControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new HomeController(new SiteProperties("https://learn.example", "Test Learning")),
                new FrontendController()).build();
    }

    @Test
    void publicHomepageLoadsOriginalVueAppWithConfiguredCanonical() throws Exception {
        for (String path : new String[]{"/", "/index.html"}) {
            var html = mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(header().doesNotExist("X-Robots-Tag"))
                    .andReturn().getResponse().getContentAsString();
            var document = org.jsoup.Jsoup.parse(html);
            assertThat(document.select("#app")).hasSize(1);
            assertThat(document.select("script[type=module]")).isNotEmpty();
            assertThat(document.select("link[rel=canonical]").eachAttr("href"))
                    .containsExactly("https://learn.example/");
            assertThat(html).doesNotContain("th:");
        }
    }

    @Test
    void workspaceAndAccountRoutesKeepServingVueWithNoindexHeader() throws Exception {
        for (String path : new String[]{"/login", "/register", "/dashboard/youtube", "/dashboard/billing", "/shadowing", "/lingq-lab", "/transcribe"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(header().string("X-Robots-Tag", "noindex, follow"))
                    .andExpect(content().contentTypeCompatibleWith("text/html"));
        }
    }
}
