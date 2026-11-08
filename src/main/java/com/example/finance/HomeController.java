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
