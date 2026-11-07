package com.example.finance.seo;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Keep the sitemap origin aligned with canonical URLs in every environment. */
@RestController
public class RobotsController {
    private final SiteProperties site;

    public RobotsController(SiteProperties site) { this.site = site; }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        // Allow crawlers to read noindex response headers on workspace routes.
        return "User-agent: *\nAllow: /\n\nSitemap: " + site.url("/sitemap.xml") + "\n";
    }
}
