package com.example.finance.seo;

import com.example.finance.followread.YoutubeVideo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class SitemapController {

    @Autowired
    private GuideCatalog guideCatalog;

    @Autowired private SiteProperties site;
    @Autowired private PublicVideoCatalog publicVideos;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String generateSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 仅包含可被搜索引擎索引的公开 SEO 页面
        xml.append(buildUrl(site.url("/"), "daily", "1.0"));
        xml.append(buildUrl(site.url("/video"), "daily", "0.9"));

        xml.append(buildUrl(site.url("/guides"), "monthly", "0.8"));
        for (GuideCatalog.Guide guide : guideCatalog.all()) {
            xml.append(buildUrl(site.url(guide.getPath()), "monthly", "0.7"));
        }

        // 动态生成视频页面
        List<YoutubeVideo> videos = publicVideos.all();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (YoutubeVideo video : videos) {
            xml.append(buildUrlWithLastmod(
                site.url("/video/" + video.getVideoId()),
                formatLastmod(video, formatter),
                "weekly",
                "0.8"
            ));
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private String buildUrl(String loc, String changefreq, String priority) {
        return String.format(
            "  <url>\n    <loc>%s</loc>\n    <changefreq>%s</changefreq>\n    <priority>%s</priority>\n  </url>\n",
            escapeXml(loc), changefreq, priority
        );
    }

    private String buildUrlWithLastmod(String loc, String lastmod, String changefreq, String priority) {
        if (lastmod == null) return buildUrl(loc, changefreq, priority);
        return String.format(
            "  <url>\n    <loc>%s</loc>\n    <lastmod>%s</lastmod>\n    <changefreq>%s</changefreq>\n    <priority>%s</priority>\n  </url>\n",
            escapeXml(loc), lastmod, changefreq, priority
        );
    }

    private String escapeXml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }

    private String formatLastmod(YoutubeVideo video, DateTimeFormatter formatter) {
        LocalDateTime timestamp = video.getCompletedAt() != null
            ? video.getCompletedAt()
            : video.getCreatedAt();
        if (timestamp != null) {
            return timestamp.format(formatter);
        }
        return null;
    }
}
