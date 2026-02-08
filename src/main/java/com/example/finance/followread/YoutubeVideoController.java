package com.example.finance.followread;

import com.example.finance.User;
import com.example.finance.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/youtube")
@CrossOrigin(origins = "*")
public class YoutubeVideoController {

    @Autowired
    private YoutubeVideoService youtubeVideoService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 检查系统状态（yt-dlp 是否安装）
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("ytDlpInstalled", youtubeVideoService.isYtDlpInstalled());
        status.put("ready", youtubeVideoService.isYtDlpInstalled());
        return ResponseEntity.ok(status);
    }

    /**
     * 添加视频到库（仅获取基本信息）
     */
    @PostMapping("/add")
    public ResponseEntity<?> addVideo(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String url = request.get("url");
        String difficulty = request.get("difficulty");
        
        if (url == null || url.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "YouTube URL is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // 提取视频 ID
            String videoId = youtubeVideoService.extractVideoId(url);
            
            // 获取当前用户 ID
            Long userId = getUserId(authentication);
            
            // 添加视频到库
            YoutubeVideo video = youtubeVideoService.addVideoToLibrary(url, userId, difficulty);
            
            // 异步获取视频基本信息
            youtubeVideoService.fetchVideoInfoAsync(video.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", videoId);
            response.put("taskId", video.getId());
            response.put("status", video.getStatus());
            response.put("message", "Video added to library successfully.");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid YouTube URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to add video: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 解析视频字幕（已添加的视频）
     */
    @PostMapping("/videos/{id}/parse-subtitles")
    public ResponseEntity<?> parseSubtitles(@PathVariable Long id) {
        try {
            Optional<YoutubeVideo> videoOpt = youtubeVideoService.getVideoDetails(id)
                .entrySet().stream()
                .filter(e -> "video".equals(e.getKey()))
                .map(e -> (YoutubeVideo) e.getValue())
                .findFirst();
            
            if (!videoOpt.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Video not found");
                return ResponseEntity.notFound().build();
            }
            
            YoutubeVideo video = videoOpt.get();
            
            // 检查状态
            if ("parsing".equals(video.getStatus())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Video is already being parsed");
                return ResponseEntity.badRequest().body(error);
            }
            
            if ("completed".equals(video.getStatus())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Subtitles already parsed");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 异步开始解析字幕
            youtubeVideoService.parseSubtitlesAsync(video.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", video.getId());
            response.put("message", "Subtitle parsing started");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to parse subtitles: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取视频解析状态
     */
    @GetMapping("/videos/{id}/status")
    public ResponseEntity<?> getVideoStatus(@PathVariable Long id) {
        try {
            Map<String, Object> details = youtubeVideoService.getVideoDetails(id);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Video not found");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取所有视频（供所有用户学习，支持分页）
     */
    @GetMapping("/videos")
    public ResponseEntity<?> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Page<YoutubeVideo> videoPage = youtubeVideoService.getAllVideosPaged(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", videoPage.getContent());
        response.put("totalElements", videoPage.getTotalElements());
        response.put("totalPages", videoPage.getTotalPages());
        response.put("currentPage", videoPage.getNumber());
        response.put("pageSize", videoPage.getSize());
        response.put("hasNext", videoPage.hasNext());
        response.put("hasPrevious", videoPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取视频详情（包括生成的句子）
     */
    @GetMapping("/videos/{id}")
    public ResponseEntity<?> getVideoDetails(@PathVariable Long id) {
        try {
            Map<String, Object> details = youtubeVideoService.getVideoDetails(id);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取视频的学习句子列表
     */
    @GetMapping("/videos/{id}/sentences")
    public ResponseEntity<?> getVideoSentences(@PathVariable Long id) {
        try {
            Map<String, Object> details = youtubeVideoService.getVideoDetails(id);
            return ResponseEntity.ok(details.get("sentences"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Video not found");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除视频及其所有相关数据
     */
    @DeleteMapping("/videos/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id) {
        try {
            youtubeVideoService.deleteVideo(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video and all related data deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete video: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 切换视频置顶状态
     */
    @PutMapping("/videos/{id}/pin")
    public ResponseEntity<?> togglePin(@PathVariable Long id) {
        try {
            YoutubeVideo video = youtubeVideoService.togglePin(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("pinned", video.getPinned());
            response.put("message", Boolean.TRUE.equals(video.getPinned()) ? "Video pinned" : "Video unpinned");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to toggle pin: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 从请求中获取用户 ID
     */
    private Long getUserId(Authentication authentication) {
        Long userId = 0L; // 默认匿名用户
        
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                userId = userOpt.get().getId();
            }
        }
        
        return userId;
    }
}

