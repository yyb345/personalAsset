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
            log.info("📥 收到字幕解析请求: videoId={}, videoUrl={}", 
                request.getVideoId(), request.getVideoUrl());

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
            
            log.info("🆕 创建新视频解析任务: videoId={}", request.getVideoId());
            
            // 添加到库并触发解析
            YoutubeVideo video = videoService.addVideoToLibrary(
                videoUrl, 
                1L,  // 默认用户ID（插件用户）
                "auto"
            );
            
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
     * 请求 DTO
     */
    public static class ParseRequest {
        private String videoId;
        private String videoUrl;

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
    }
}

