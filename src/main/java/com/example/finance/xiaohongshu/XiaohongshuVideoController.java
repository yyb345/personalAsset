package com.example.finance.xiaohongshu;

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
@RequestMapping("/api/xiaohongshu")
@CrossOrigin(origins = "*")
public class XiaohongshuVideoController {

    @Autowired
    private XiaohongshuVideoService xiaohongshuVideoService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 检查系统状态（yt-dlp 是否安装）
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("ytDlpInstalled", xiaohongshuVideoService.isYtDlpInstalled());
        status.put("ready", xiaohongshuVideoService.isYtDlpInstalled());
        return ResponseEntity.ok(status);
    }

    /**
     * 添加视频到库（获取基本信息）
     */
    @PostMapping("/add")
    public ResponseEntity<?> addVideo(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String url = request.get("url");
        String difficulty = request.get("difficulty");
        
        if (url == null || url.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "小红书 URL is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // 提取视频 ID
            String videoId = xiaohongshuVideoService.extractVideoId(url);
            
            // 获取当前用户 ID
            Long userId = getUserId(authentication);
            
            // 添加视频到库
            XiaohongshuVideo video = xiaohongshuVideoService.addVideoToLibrary(url, userId, difficulty);
            
            // 异步获取视频基本信息
            xiaohongshuVideoService.fetchVideoInfoAsync(video.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", videoId);
            response.put("taskId", video.getId());
            response.put("status", video.getStatus());
            response.put("message", "Video added to library successfully.");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid 小红书 URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to add video: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取视频解析状态
     */
    @GetMapping("/videos/{id}/status")
    public ResponseEntity<?> getVideoStatus(@PathVariable Long id) {
        try {
            Map<String, Object> details = xiaohongshuVideoService.getVideoDetails(id);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Video not found");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取用户的所有视频任务（支持分页）
     */
    @GetMapping("/videos")
    public ResponseEntity<?> getUserVideos(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserId(authentication);
        org.springframework.data.domain.Page<XiaohongshuVideo> videoPage = xiaohongshuVideoService.getUserVideosPaged(userId, page, size);

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
     * 获取视频详情
     */
    @GetMapping("/videos/{id}")
    public ResponseEntity<?> getVideoDetails(@PathVariable Long id) {
        try {
            Map<String, Object> details = xiaohongshuVideoService.getVideoDetails(id);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除视频及其所有相关数据
     */
    @DeleteMapping("/videos/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id) {
        try {
            xiaohongshuVideoService.deleteVideo(id);
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

