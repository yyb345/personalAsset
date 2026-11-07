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

/** Serve the original Vue landing page prerendered during the frontend build. */
@RestController
public class HomeController {
    private final SiteProperties site;

    public HomeController(SiteProperties site) {
        this.site = site;
    }

    @GetMapping({"/", "/index.html"})
    public ResponseEntity<String> home() throws IOException {
        var resource = new ClassPathResource("static/home.html");
        if (!resource.exists()) return ResponseEntity.notFound().build();
        var document = Jsoup.parse(resource.getContentAsString(StandardCharsets.UTF_8));
        document.select("link[rel=canonical]").remove();
        document.head().appendElement("link").attr("rel", "canonical").attr("href", site.url("/"));
        document.select("meta[property=og:url]").attr("content", site.url("/"));
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(document.outerHtml());
    }
}
