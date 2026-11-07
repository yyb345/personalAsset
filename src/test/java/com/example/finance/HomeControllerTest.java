package com.example.finance;

import com.example.finance.seo.SiteProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HomeControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new HomeController(new SiteProperties("https://learn.example", "Test Learning")),
                new FrontendController()).setViewResolvers(views).build();
    }

    @Test
    void publicHomepageIncludesContentAndConfiguredCanonicalWithoutJavascript() throws Exception {
        for (String path : new String[]{"/", "/index.html"}) {
            var html = mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(header().doesNotExist("X-Robots-Tag"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).contains("<h1>学 AI，", "也练英语。", "Test Learning",
                    "rel=\"canonical\" href=\"https://learn.example/\"", "href=\"/video\"",
                    "href=\"/guides\"", "href=\"/dashboard/youtube\"");
            assertThat(html).doesNotContain("th:", "<div id=\"app\">");
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
