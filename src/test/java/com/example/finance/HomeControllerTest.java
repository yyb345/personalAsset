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
    void publicHomepageIncludesOriginalContentWithoutJavascriptAndConfiguredCanonical() throws Exception {
        for (String path : new String[]{"/", "/index.html"}) {
            var html = mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(header().doesNotExist("X-Robots-Tag"))
                    .andReturn().getResponse().getContentAsString();
            var document = org.jsoup.Jsoup.parse(html);
            assertThat(document.select("#app")).hasSize(1);
            assertThat(document.select("#app").attr("data-prerendered")).isEqualTo("true");
            assertThat(document.select("h1").text()).contains("Explore AI.", "English.");
            assertThat(document.select("html").attr("lang")).isEqualTo("en");
            assertThat(document.select(".locale-select option").eachAttr("value"))
                    .containsExactly("en-US", "zh-CN");
            assertThat(document.select("a[href=/guides]")).isNotEmpty();
            assertThat(document.select("a[href=/video]")).isNotEmpty();
            assertThat(document.select(".demo-tabs button")).hasSize(2);
            assertThat(document.select("script[type=module]")).isNotEmpty();
            assertThat(document.select("link[rel=canonical]").eachAttr("href"))
                    .containsExactly("https://learn.example/");
            assertAlternates(document);
            assertThat(html).doesNotContain("th:");
        }
    }

    @Test
    void chineseHomepageHasChineseBodyAndSelfCanonicalBeforeJavascript() throws Exception {
        var html = mvc.perform(get("/zh/")).andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Robots-Tag"))
                .andReturn().getResponse().getContentAsString();
        var document = org.jsoup.Jsoup.parse(html);
        assertThat(document.select("html").attr("lang")).isEqualTo("zh-CN");
        assertThat(document.title()).contains("学 AI，也练英语");
        assertThat(document.select("h1").text()).contains("学 AI", "英语");
        assertThat(document.select(".hero-description").text()).contains("笔记", "词卡复习");
        assertThat(document.select("#app").attr("data-prerendered")).isEqualTo("true");
        assertThat(document.select("link[rel=canonical]").eachAttr("href"))
                .containsExactly("https://learn.example/zh/");
        assertThat(document.select("meta[property=og:url]").attr("content"))
                .isEqualTo("https://learn.example/zh/");
        assertThat(document.select("meta[name=description]").attr("content")).contains("英文视频", "查词");
        assertThat(document.select(".brand-logo").first().attr("href")).isEqualTo("/zh/");
        assertAlternates(document);
    }

    @Test
    void chineseUrlWithoutTrailingSlashPermanentlyRedirects() throws Exception {
        mvc.perform(get("/zh")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/zh/"));
    }

    @Test
    void articlesHaveLocalizedContentAndConfiguredSeoUrls() throws Exception {
        String slug = "/articles/jeff-dean-discovery-loop-ai-scientific-discovery";
        for (String prefix : new String[]{"", "/zh"}) {
            String path = prefix + slug;
            var html = mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Robots-Tag"))
                .andReturn().getResponse().getContentAsString();
            var document = org.jsoup.Jsoup.parse(html);
            assertThat(document.select("h1")).hasSize(1);
            assertThat(document.select("h1").text()).contains("Discovery Loop");
            assertThat(document.select("html").attr("lang")).isEqualTo(prefix.isEmpty() ? "en" : "zh-CN");
            assertThat(document.select("link[rel=canonical]").attr("href")).isEqualTo("https://learn.example" + path);
            assertThat(document.select("link[hreflang=en]").attr("href")).isEqualTo("https://learn.example" + slug);
            assertThat(document.select("link[hreflang=zh-Hans]").attr("href")).isEqualTo("https://learn.example/zh" + slug);
            var schema = new com.fasterxml.jackson.databind.ObjectMapper().readTree(document.getElementById("article-schema").data());
            assertThat(schema.get("mainEntityOfPage").asText()).isEqualTo("https://learn.example" + path);
            assertThat(document.select("article section")).hasSize(10);
        }
    }

    private void assertAlternates(org.jsoup.nodes.Document document) {
        assertThat(document.select("link[rel=alternate][hreflang]")).hasSize(3);
        assertThat(document.select("link[hreflang=en]").attr("href")).isEqualTo("https://learn.example/");
        assertThat(document.select("link[hreflang=zh-Hans]").attr("href")).isEqualTo("https://learn.example/zh/");
        assertThat(document.select("link[hreflang=x-default]").attr("href")).isEqualTo("https://learn.example/");
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
