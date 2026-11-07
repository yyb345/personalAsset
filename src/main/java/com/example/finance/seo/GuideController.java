package com.example.finance.seo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Map;

@Controller
public class GuideController {
    private final GuideCatalog catalog;
    private final ObjectMapper mapper;
    private final SiteProperties site;
    public GuideController(GuideCatalog catalog, ObjectMapper mapper, SiteProperties site) {
        this.catalog = catalog;
        this.mapper = mapper;
        this.site = site;
    }

    @GetMapping("/guides")
    public String index(Model model) {
        model.addAttribute("pageUrl", site.url("/guides"));
        model.addAttribute("guides", catalog.all());
        return "seo/guides";
    }

    @GetMapping("/guides/{slug}")
    public String guide(@PathVariable String slug, Model model, HttpServletResponse response)
            throws JsonProcessingException {
        var found = catalog.find(slug);
        if (found.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("guides", catalog.all());
            model.addAttribute("notFound", true);
            return "seo/guides";
        }
        var guide = found.get();
        model.addAttribute("guide", guide);
        model.addAttribute("pageUrl", site.url(guide.getPath()));
        model.addAttribute("related", catalog.all().stream().filter(g -> !g.slug().equals(slug)).toList());
        var breadcrumbs = Map.of("@type", "BreadcrumbList", "itemListElement", List.of(
            Map.of("@type", "ListItem", "position", 1, "name", "Home", "item", site.url("/")),
            Map.of("@type", "ListItem", "position", 2, "name", "English learning guides", "item", site.url("/guides")),
            Map.of("@type", "ListItem", "position", 3, "name", guide.title(), "item", site.url(guide.getPath()))
        ));
        var article = Map.of("@type", "Article", "headline", guide.title(), "description", guide.description(),
            "inLanguage", "en", "mainEntityOfPage", site.url(guide.getPath()), "url", site.url(guide.getPath()),
            "author", Map.of("@type", "Organization", "name", site.getName(), "url", site.url("/")));
        String json = mapper.writeValueAsString(Map.of("@context", "https://schema.org", "@graph", List.of(article, breadcrumbs)));
        model.addAttribute("jsonLd", json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026"));
        return "seo/guide";
    }
}
