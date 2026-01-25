package com.example.finance.followread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Shadowing API Controller - 专为 Chrome 插件设计的轻量级 API
 * 提供快速字幕解析和跟读数据
 */
@RestController
@RequestMapping("/api/youtube")
@CrossOrigin(origins = "*")
public class ShadowingApiController {

    private static final Logger log = LoggerFactory.getLogger(ShadowingApiController.class);

    @Autowired
    private YoutubeVideoService videoService;

    @Autowired
    private YoutubeVideoRepository videoRepository;

    @Autowired
    private FollowReadSentenceRepository sentenceRepository;

    /**
     * 解析视频字幕（Chrome 插件专用）
     * POST /api/youtube/parse
     */
    @PostMapping("/parse")
    public ResponseEntity<?> parseSubtitles(@RequestBody ParseRequest request) {
        try {
            log.info("📥 收到字幕解析请求: videoId={}, videoUrl={},cookie={}",
                request.getVideoId(), request.getVideoUrl(),request.getCookies());

            // 校验参数
            if (request.getVideoId() == null || request.getVideoId().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "videoId is required"));
            }

            // 检查是否已经解析过
            Optional<YoutubeVideo> existingVideo = videoRepository.findByVideoId(request.getVideoId());
            
            if (existingVideo.isPresent()) {
                YoutubeVideo video = existingVideo.get();
                
                // 如果已经完成解析，直接返回数据
                if ("completed".equals(video.getStatus())) {
                    log.info("✅ 视频已解析过，直接返回: videoId={}", request.getVideoId());
                    return ResponseEntity.ok(buildResponse(video));
                }
                
                // 如果正在解析，返回状态
                if ("parsing".equals(video.getStatus())) {
                    log.info("⏳ 视频正在解析中: videoId={}", request.getVideoId());
                    return ResponseEntity.ok(Map.of(
                        "status", "parsing",
                        "message", video.getProgressMessage(),
                        "videoId", video.getVideoId()
                    ));
                }
                
                // 如果之前失败了，重新尝试解析
                if ("failed".equals(video.getStatus()) || "added".equals(video.getStatus())) {
                    log.info("🔄 重新解析视频: videoId={}", request.getVideoId());
                    videoService.parseSubtitlesAsync(video.getId());
                    
                    return ResponseEntity.ok(Map.of(
                        "status", "parsing",
                        "message", "开始重新解析字幕...",
                        "videoId", video.getVideoId()
                    ));
                }
            }

            // 创建新的视频记录
            String videoUrl = request.getVideoUrl() != null ? 
                request.getVideoUrl() : 
                "https://www.youtube.com/watch?v=" + request.getVideoId();
            
            log.info("🆕 创建新视频解析任务: videoId={}, cookies={}", 
                request.getVideoId(), request.getCookies() != null ? "已提供" : "未提供");
            
            // 添加到库并触发解析
            YoutubeVideo video = videoService.addVideoToLibrary(
                videoUrl, 
                1L,  // 默认用户ID（插件用户）
                "auto"
            );
            
            // 如果提供了 cookies，先保存
            if (request.getCookies() != null && !request.getCookies().isEmpty()) {
                videoService.saveCookiesForVideo(video.getVideoId(), request.getCookies());
            }
            
            // 异步解析字幕
            videoService.parseSubtitlesAsync(video.getId());
            
            return ResponseEntity.ok(Map.of(
                "status", "parsing",
                "message", "开始解析字幕，请稍后...",
                "videoId", video.getVideoId(),
                "taskId", video.getId()
            ));
            
        } catch (Exception e) {
            log.error("❌ 字幕解析请求失败: videoId={}", request.getVideoId(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "解析失败: " + e.getMessage()));
        }
    }

    /**
     * 查询解析状态
     * GET /api/youtube/status/{videoId}
     */
    @GetMapping("/status/{videoId}")
    public ResponseEntity<?> getParseStatus(@PathVariable String videoId) {
        try {
            Optional<YoutubeVideo> videoOpt = videoRepository.findByVideoId(videoId);
            
            if (!videoOpt.isPresent()) {
                return ResponseEntity.status(404)
                    .body(Map.of("error", "视频未找到"));
            }
            
            YoutubeVideo video = videoOpt.get();
            
            if ("completed".equals(video.getStatus())) {
                return ResponseEntity.ok(buildResponse(video));
            } else if ("parsing".equals(video.getStatus())) {
                return ResponseEntity.ok(Map.of(
                    "status", "parsing",
                    "message", video.getProgressMessage(),
                    "videoId", video.getVideoId()
                ));
            } else if ("failed".equals(video.getStatus())) {
                return ResponseEntity.ok(Map.of(
                    "status", "failed",
                    "message", video.getErrorMessage(),
                    "videoId", video.getVideoId()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "status", video.getStatus(),
                    "message", video.getProgressMessage(),
                    "videoId", video.getVideoId()
                ));
            }
            
        } catch (Exception e) {
            log.error("❌ 状态查询失败: videoId={}", videoId, e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取视频的学习句子
     * GET /api/youtube/sentences/{videoId}
     */
    @GetMapping("/sentences/{videoId}")
    public ResponseEntity<?> getSentences(@PathVariable String videoId) {
        try {
            Optional<YoutubeVideo> videoOpt = videoRepository.findByVideoId(videoId);
            
            if (!videoOpt.isPresent()) {
                return ResponseEntity.status(404)
                    .body(Map.of("error", "视频未找到"));
            }
            
            YoutubeVideo video = videoOpt.get();
            
            if (!"completed".equals(video.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "视频尚未解析完成"));
            }
            
            return ResponseEntity.ok(buildResponse(video));
            
        } catch (Exception e) {
            log.error("❌ 获取句子失败: videoId={}", videoId, e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "获取失败: " + e.getMessage()));
        }
    }

    /**
     * 构建响应数据
     */
    private Map<String, Object> buildResponse(YoutubeVideo video) {
        // 获取句子列表
        List<FollowReadSentence> sentences = sentenceRepository.findByYoutubeVideoId(video.getId());
        
        // 转换为简化的 DTO
        List<Map<String, Object>> sentenceList = new ArrayList<>();
        for (FollowReadSentence sentence : sentences) {
            Map<String, Object> sentenceData = new HashMap<>();
            sentenceData.put("id", sentence.getId());
            sentenceData.put("text", sentence.getText());
            sentenceData.put("startTime", sentence.getStartTime());
            sentenceData.put("endTime", sentence.getEndTime());
            sentenceData.put("difficulty", sentence.getDifficulty());
            sentenceData.put("order", sentence.getSentenceOrder());
            sentenceList.add(sentenceData);
        }
        
        // 按顺序排序
        sentenceList.sort((a, b) -> 
            Integer.compare((Integer) a.get("order"), (Integer) b.get("order")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("videoId", video.getVideoId());
        response.put("videoTitle", video.getTitle());
        response.put("videoUrl", video.getSourceUrl());
        response.put("duration", video.getDuration());
        response.put("channel", video.getChannel());
        response.put("thumbnailUrl", video.getThumbnailUrl());
        response.put("sentences", sentenceList);
        response.put("totalSentences", sentenceList.size());
        
        return response;
    }

    /**
     * 接收从浏览器提取的字幕（Chrome Extension 专用）
     * POST /api/youtube/parse-from-browser
     */
    @PostMapping("/parse-from-browser")
    public ResponseEntity<?> parseSubtitlesFromBrowser(@RequestBody BrowserSubtitlesRequest request) {
        try {
            log.info("📥 收到浏览器提取的字幕: videoId={}, 字幕数={}", 
                request.getVideoId(), request.getSubtitles() != null ? request.getSubtitles().size() : 0);

            // 校验参数
            if (request.getVideoId() == null || request.getVideoId().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "videoId is required"));
            }

            if (request.getSubtitles() == null || request.getSubtitles().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "subtitles are required"));
            }

            // 检查是否已经存在
            Optional<YoutubeVideo> existingVideo = videoRepository.findByVideoId(request.getVideoId());
            
            if (existingVideo.isPresent() && "completed".equals(existingVideo.get().getStatus())) {
                log.info("✅ 视频已解析过，直接返回: videoId={}", request.getVideoId());
                YoutubeVideo video = existingVideo.get();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "videoId", video.getVideoId(),
                    "sentenceCount", video.getSentenceCount(),
                    "message", "Video already processed"
                ));
            }

            // 使用 YoutubeVideoService 处理浏览器提取的字幕
            YoutubeVideo video = videoService.processSubtitlesFromBrowser(
                request.getVideoId(),
                request.getVideoUrl(),
                request.getMetadata(),
                request.getSubtitles(),
                request.getCookies()  // 传递 cookies
            );

            log.info("✅ 浏览器字幕处理完成: videoId={}, sentences={}", 
                video.getVideoId(), video.getSentenceCount());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "videoId", video.getVideoId(),
                "sentenceCount", video.getSentenceCount(),
                "message", "Subtitles processed successfully"
            ));
            
        } catch (Exception e) {
            log.error("❌ 浏览器字幕处理失败: videoId={}", request.getVideoId(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "处理失败: " + e.getMessage()));
        }
    }

    /**
     * 请求 DTO
     */
    public static class ParseRequest {
        private String videoId;
        private String videoUrl;
        private String cookies;  // 添加 cookies 字段

        public String getVideoId() {
            return videoId;
        }

        public void setVideoId(String videoId) {
            this.videoId = videoId;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getCookies() {
            return cookies;
        }

        public void setCookies(String cookies) {
            this.cookies = cookies;
        }
    }

    /**
     * 浏览器字幕请求 DTO
     */
    public static class BrowserSubtitlesRequest {
        private String videoId;
        private String videoUrl;
        private Map<String, Object> metadata;
        private List<Map<String, Object>> subtitles;
        private String cookies;  // 添加 cookies 字段

        public String getVideoId() {
            return videoId;
        }

        public void setVideoId(String videoId) {
            this.videoId = videoId;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public List<Map<String, Object>> getSubtitles() {
            return subtitles;
        }

        public void setSubtitles(List<Map<String, Object>> subtitles) {
            this.subtitles = subtitles;
        }

        public String getCookies() {
            return cookies;
        }

        public void setCookies(String cookies) {
            this.cookies = cookies;
        }
    }
}

