package com.example.finance.seo;

import com.example.finance.followread.SubtitleSegment;
import com.example.finance.followread.SubtitleSegmentRepository;
import com.example.finance.followread.YoutubeVideo;
import com.example.finance.followread.YoutubeVideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/video")
public class SeoController {

    @Autowired
    private YoutubeVideoRepository youtubeVideoRepository;

    @Autowired
    private SubtitleSegmentRepository subtitleSegmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/{videoId}")
    public String videoPage(@PathVariable String videoId, Model model, HttpServletResponse response) {
        Optional<YoutubeVideo> videoOpt = youtubeVideoRepository.findByVideoId(videoId);

        if (videoOpt.isEmpty() || !"completed".equals(videoOpt.get().getStatus())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "seo/not-found";
        }

        YoutubeVideo video = videoOpt.get();
        String pageUrl = "https://www.xlearning.top/video/" + video.getVideoId();
        model.addAttribute("video", video);
        model.addAttribute("pageUrl", pageUrl);
        model.addAttribute("youtubeUrl", "https://www.youtube.com/watch?v=" + video.getVideoId());
        model.addAttribute("seoDescription", buildSeoDescription(video));
        model.addAttribute("jsonLd", buildVideoJsonLd(video, pageUrl));

        // 获取字幕内容用于 SEO（使用 YoutubeVideo 的主键 id）
        List<SubtitleSegment> subtitles = subtitleSegmentRepository.findByVideoIdOrderBySegmentOrder(video.getId());
        model.addAttribute("subtitles", subtitles);

        // 构建完整的字幕文本用于 SEO
        StringBuilder fullText = new StringBuilder();
        for (SubtitleSegment segment : subtitles) {
            String text = segment.getCleanText() != null ? segment.getCleanText() : segment.getRawText();
            fullText.append(text).append(" ");
        }
        model.addAttribute("fullText", fullText.toString().trim());

        return "seo/video";
    }

    @GetMapping("")
    public String videoList(Model model) {
        // 获取所有已完成的视频列表
        List<YoutubeVideo> videos = youtubeVideoRepository.findByStatus("completed");
        model.addAttribute("videos", videos);
        return "seo/video-list";
    }

    private String buildSeoDescription(YoutubeVideo video) {
        String channel = video.getChannel() != null ? video.getChannel() : "YouTube";
        String description = String.format(
            "Read the full transcript of \"%s\" by %s. Practice English listening and reading with interactive subtitles on xLearning.",
            video.getTitle(),
            channel
        );
        return description.length() > 160 ? description.substring(0, 157) + "..." : description;
    }

    private String buildVideoJsonLd(YoutubeVideo video, String pageUrl) {
        try {
            ObjectNode videoNode = objectMapper.createObjectNode();
            videoNode.put("@type", "VideoObject");
            videoNode.put("name", video.getTitle());
            videoNode.put("description", buildSeoDescription(video));
            videoNode.put("url", pageUrl);
            if (video.getThumbnailUrl() != null) {
                videoNode.put("thumbnailUrl", video.getThumbnailUrl());
            }
            if (video.getDuration() != null) {
                videoNode.put("duration", "PT" + video.getDuration() + "S");
            }
            if (video.getCreatedAt() != null) {
                videoNode.put("uploadDate", video.getCreatedAt().toString());
            }
            videoNode.put("embedUrl", "https://www.youtube.com/embed/" + video.getVideoId());
            videoNode.put("contentUrl", "https://www.youtube.com/watch?v=" + video.getVideoId());

            ArrayNode breadcrumbItems = objectMapper.createArrayNode();
            ObjectNode home = objectMapper.createObjectNode();
            home.put("@type", "ListItem");
            home.put("position", 1);
            home.put("name", "Home");
            home.put("item", "https://www.xlearning.top/");
            breadcrumbItems.add(home);

            ObjectNode videos = objectMapper.createObjectNode();
            videos.put("@type", "ListItem");
            videos.put("position", 2);
            videos.put("name", "Videos");
            videos.put("item", "https://www.xlearning.top/video");
            breadcrumbItems.add(videos);

            ObjectNode current = objectMapper.createObjectNode();
            current.put("@type", "ListItem");
            current.put("position", 3);
            current.put("name", video.getTitle());
            current.put("item", pageUrl);
            breadcrumbItems.add(current);

            ObjectNode breadcrumb = objectMapper.createObjectNode();
            breadcrumb.put("@type", "BreadcrumbList");
            breadcrumb.set("itemListElement", breadcrumbItems);

            ObjectNode webPage = objectMapper.createObjectNode();
            webPage.put("@type", "WebPage");
            webPage.put("name", video.getTitle() + " - YouTube Transcript");
            webPage.put("description", buildSeoDescription(video));
            webPage.put("url", pageUrl);
            webPage.set("mainEntity", videoNode);

            ArrayNode graph = objectMapper.createArrayNode();
            graph.add(webPage);
            graph.add(videoNode);
            graph.add(breadcrumb);

            ObjectNode root = objectMapper.createObjectNode();
            root.put("@context", "https://schema.org");
            root.set("@graph", graph);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }
}
