package com.example.finance;

import com.example.finance.seo.SiteProperties;
import org.jsoup.Jsoup;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URI;

/** Serve the original Vue landing page prerendered during the frontend build. */
@RestController
public class HomeController {
    private final SiteProperties site;

    public HomeController(SiteProperties site) {
        this.site = site;
    }

    @GetMapping({"/", "/index.html"})
    public ResponseEntity<String> home() throws IOException {
        return renderHome("home.html", "/");
    }

    @GetMapping("/zh/")
    public ResponseEntity<String> chineseHome() throws IOException {
        return renderHome("home-zh.html", "/zh/");
    }

    @GetMapping("/zh")
    public ResponseEntity<Void> chineseHomeRedirect() {
        return ResponseEntity.status(301).location(URI.create("/zh/")).build();
    }

    @GetMapping("/articles/jeff-dean-discovery-loop-ai-scientific-discovery")
    public ResponseEntity<String> discoveryLoopEnglish() throws IOException {
        return renderArticle("article-discovery-loop-en.html", "/articles/jeff-dean-discovery-loop-ai-scientific-discovery");
    }

    @GetMapping("/zh/articles/jeff-dean-discovery-loop-ai-scientific-discovery")
    public ResponseEntity<String> discoveryLoopChinese() throws IOException {
        return renderArticle("article-discovery-loop-zh.html", "/zh/articles/jeff-dean-discovery-loop-ai-scientific-discovery");
    }

    private ResponseEntity<String> renderArticle(String file, String path) throws IOException {
        var resource = new ClassPathResource("static/" + file);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        var document = Jsoup.parse(resource.getContentAsString(StandardCharsets.UTF_8));
        document.select("link[rel=canonical]").attr("href", site.url(path));
        document.select("meta[property=og:url]").attr("content", site.url(path));
        document.select("link[rel=alternate][hreflang]").forEach(link -> {
            String alternatePath = "zh-Hans".equals(link.attr("hreflang"))
                ? "/zh/articles/jeff-dean-discovery-loop-ai-scientific-discovery"
                : "/articles/jeff-dean-discovery-loop-ai-scientific-discovery";
            link.attr("href", site.url(alternatePath));
        });
        var schema = document.getElementById("article-schema");
        if (schema != null) {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(schema.data());
            json.put("mainEntityOfPage", site.url(path));
            schema.html(mapper.writeValueAsString(json).replace("<", "\\u003c"));
        }
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .body(document.outerHtml());
    }

    private ResponseEntity<String> renderHome(String file, String path) throws IOException {
        var resource = new ClassPathResource("static/" + file);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        var document = Jsoup.parse(resource.getContentAsString(StandardCharsets.UTF_8));
        document.select("link[rel=canonical]").remove();
        document.head().appendElement("link").attr("rel", "canonical").attr("href", site.url(path));
        document.select("meta[property=og:url]").attr("content", site.url(path));
        document.select("link[rel=alternate][hreflang]").remove();
        for (String[] alternate : new String[][] {{"en", "/"}, {"zh-Hans", "/zh/"}, {"x-default", "/"}}) {
            document.head().appendElement("link").attr("rel", "alternate")
                    .attr("hreflang", alternate[0]).attr("href", site.url(alternate[1]));
        }
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(document.outerHtml());
    }
}
