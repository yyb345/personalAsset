package com.example.finance.seo;

import com.example.finance.followread.YoutubeVideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import java.util.HashSet;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GuideControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private GuideCatalog catalog;
    private MockMvc mvc;

    @BeforeEach
    void setup() throws Exception {
        catalog = new GuideCatalog(mapper);
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
        mvc = MockMvcBuilders.standaloneSetup(new GuideController(catalog, mapper, new SiteProperties("https://www.xlearning.top", "xLearning"))).setViewResolvers(views).build();
    }

    @Test
    void allTenGuidesRenderCompleteHtmlAndMatchingStructuredData() throws Exception {
        assertThat(catalog.all()).hasSize(10);
        var titles = new HashSet<String>();
        var descriptions = new HashSet<String>();
        for (var guide : catalog.all()) {
            assertThat(titles.add(guide.title())).isTrue();
            assertThat(descriptions.add(guide.description())).isTrue();
            var html = mvc.perform(get("/guides/" + guide.slug())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            var readable = org.springframework.web.util.HtmlUtils.htmlUnescape(html);
            assertThat(readable).contains("<h1>" + guide.title() + "</h1>",
                "<link rel=" + "\"canonical\" href=\"" + "https://www.xlearning.top" + guide.getPath() + "\">", guide.description(), guide.intro(), guide.exercise(), guide.answer());
            for (var section : guide.sections()) assertThat(readable).contains(section.body(), section.example());
            var json = html.substring(html.indexOf("<script type=\"application/ld+json\">") + "<script type=\"application/ld+json\">".length());
            var graph = mapper.readTree(json.substring(0, json.indexOf("</script>"))).get("@graph");
            assertThat(graph.get(0).get("headline").asText()).isEqualTo(guide.title());
            assertThat(graph.get(1).get("itemListElement").get(2).get("item").asText()).isEqualTo("https://www.xlearning.top" + guide.getPath());
        }
    }

    @Test
    void hubLinksEveryGuideAndMissingGuideReturns404() throws Exception {
        var html = mvc.perform(get("/guides")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (var guide : catalog.all()) assertThat(html).contains("href=\"/guides/" + guide.slug() + "\"");
        var missing = mvc.perform(get("/guides/missing-guide")).andExpect(status().isNotFound())
            .andReturn().getResponse().getContentAsString();
        assertThat(missing).contains("name=\"robots\" content=\"noindex, follow\"");
    }

    @Test
    void sitemapContainsAllGuides() throws Exception {
        var repository = mock(YoutubeVideoRepository.class);
        when(repository.findPublicVideos()).thenReturn(List.of());
        var controller = new SitemapController();
        ReflectionTestUtils.setField(controller, "guideCatalog", catalog);
        ReflectionTestUtils.setField(controller, "site", new SiteProperties("https://www.xlearning.top", "xLearning"));
        ReflectionTestUtils.setField(controller, "publicVideos", new PublicVideoCatalog(repository));
        var xml = controller.generateSitemap();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThat(document.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "url").getLength()).isEqualTo(13);
        for (var guide : catalog.all()) assertThat(xml).contains("<loc>" + "https://www.xlearning.top" + guide.getPath() + "</loc>");
    }
}
