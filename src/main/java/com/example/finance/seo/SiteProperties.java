package com.example.finance.seo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;

@Component
public class SiteProperties {
    private final String baseUrl;
    private final String name;
    public SiteProperties(@Value("${site.base-url:https://www.xlearning.top}") String baseUrl,
                          @Value("${site.name:xLearning}") String name) {
        URI uri = URI.create(baseUrl);
        if (!isHttpScheme(uri.getScheme()) || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))) {
            throw new IllegalArgumentException("site.base-url must be an absolute HTTP(S) origin");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.name = name;
    }
    private static boolean isHttpScheme(String scheme) { return "https".equals(scheme) || "http".equals(scheme); }
    public String getBaseUrl() { return baseUrl; }
    public String getName() { return name; }
    public String url(String path) { return baseUrl + (path.startsWith("/") ? path : "/" + path); }
}
