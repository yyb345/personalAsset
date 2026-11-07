package com.example.finance;

import com.example.finance.seo.SiteProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Public landing page rendered without requiring JavaScript. */
@Controller
public class HomeController {
    private final SiteProperties site;

    public HomeController(SiteProperties site) {
        this.site = site;
    }

    @GetMapping({"/", "/index.html"})
    public String home(Model model) {
        model.addAttribute("siteName", site.getName());
        model.addAttribute("canonicalUrl", site.url("/"));
        return "seo/home";
    }
}
