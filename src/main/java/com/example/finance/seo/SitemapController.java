package com.example.finance.seo;

import com.example.finance.followread.YoutubeVideo;
import com.example.finance.followread.YoutubeVideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class SitemapController {

    @Autowired
    private YoutubeVideoRepository youtubeVideoRepository;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String generateSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 静态页面
        xml.append(buildUrl("https://www.xlearning.top/", "daily", "1.0"));
        xml.append(buildUrl("https://www.xlearning.top/dashboard/youtube", "daily", "0.9"));
        xml.append(buildUrl("https://www.xlearning.top/dashboard/xiaohongshu", "daily", "0.8"));
        xml.append(buildUrl("https://www.xlearning.top/shadowing", "weekly", "0.8"));
        xml.append(buildUrl("https://www.xlearning.top/transcribe", "weekly", "0.8"));
        xml.append(buildUrl("https://www.xlearning.top/video", "daily", "0.9"));

        // 动态生成视频页面
        List<YoutubeVideo> videos = youtubeVideoRepository.findByStatus("completed");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (YoutubeVideo video : videos) {
            String lastmod = video.getCompletedAt() != null
                ? video.getCompletedAt().format(formatter)
                : video.getCreatedAt().format(formatter);
            xml.append(buildUrlWithLastmod(
                "https://www.xlearning.top/video/" + video.getVideoId(),
                lastmod,
                "weekly",
                "0.7"
            ));
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private String buildUrl(String loc, String changefreq, String priority) {
        return String.format(
            "  <url>\n    <loc>%s</loc>\n    <changefreq>%s</changefreq>\n    <priority>%s</priority>\n  </url>\n",
            loc, changefreq, priority
        );
    }

    private String buildUrlWithLastmod(String loc, String lastmod, String changefreq, String priority) {
        return String.format(
            "  <url>\n    <loc>%s</loc>\n    <lastmod>%s</lastmod>\n    <changefreq>%s</changefreq>\n    <priority>%s</priority>\n  </url>\n",
            loc, lastmod, changefreq, priority
        );
    }
}
