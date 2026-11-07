package com.example.finance.seo;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RobotsControllerTest {
    @Test void sitemapUsesTheConfiguredCanonicalOrigin() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new RobotsController(
            new SiteProperties("https://example.com/", "Example"))).build();
        mvc.perform(get("/robots.txt")).andExpect(status().isOk())
            .andExpect(content().string("User-agent: *\nAllow: /\n\nSitemap: https://example.com/sitemap.xml\n"));
    }
}
