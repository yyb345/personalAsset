package com.example.finance.seo;

import com.example.finance.followread.SubtitleSegment;
import com.example.finance.followread.SubtitleSegmentRepository;
import com.example.finance.followread.YoutubeVideo;
import com.example.finance.followread.YoutubeVideoRepository;
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

    @GetMapping("/{videoId}")
    public String videoPage(@PathVariable String videoId, Model model) {
        Optional<YoutubeVideo> videoOpt = youtubeVideoRepository.findByVideoId(videoId);

        if (videoOpt.isEmpty()) {
            return "seo/not-found";
        }

        YoutubeVideo video = videoOpt.get();
        model.addAttribute("video", video);

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
}
