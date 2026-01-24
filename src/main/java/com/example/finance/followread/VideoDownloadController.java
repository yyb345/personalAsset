package com.example.finance.followread;

import com.example.finance.User;
import com.example.finance.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 视频下载 API 控制器
 */
@RestController
@RequestMapping("/api/youtube/download")
@CrossOrigin(origins = "*")
public class VideoDownloadController {

    private static final Logger log = LoggerFactory.getLogger(VideoDownloadController.class);

    @Autowired
    private VideoDownloadService downloadService;

    @Autowired
    private YoutubeVideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取视频的可用格式列表
     */
    @GetMapping("/formats/{videoId}")
    public ResponseEntity<?> getAvailableFormats(@PathVariable Long videoId) {
        try {
            Optional<YoutubeVideo> videoOpt = videoRepository.findById(videoId);
            if (!videoOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            YoutubeVideo video = videoOpt.get();
            List<VideoFormat> formats = downloadService.getAvailableFormats(video.getSourceUrl());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("formats", formats);
            response.put("videoId", video.getVideoId());
            response.put("title", video.getTitle());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取视频格式失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch formats: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 创建下载任务
     */
    @PostMapping("/start")
    public ResponseEntity<?> startDownload(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            Long videoId = Long.valueOf(request.get("videoId").toString());
            String downloadType = (String) request.getOrDefault("downloadType", "video");
            String formatId = (String) request.get("formatId");
            String quality = (String) request.getOrDefault("quality", "best");
            
            Long userId = getUserId(authentication);
            
            // 创建任务
            VideoDownloadTask task = downloadService.createDownloadTask(
                videoId, downloadType, formatId, quality, userId
            );
            
            // 异步开始下载
            downloadService.executeDownloadAsync(task.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", task.getId());
            response.put("status", task.getStatus());
            response.put("message", "Download task created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建下载任务失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create download task: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取下载任务状态
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<?> getTaskStatus(@PathVariable Long taskId) {
        try {
            VideoDownloadTask task = downloadService.getTaskDetails(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("task", task);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Task not found");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取用户的所有下载任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<?> getUserTasks(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<VideoDownloadTask> tasks = downloadService.getUserDownloadTasks(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tasks", tasks);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取视频的所有下载任务
     */
    @GetMapping("/video/{videoId}/tasks")
    public ResponseEntity<?> getVideoTasks(@PathVariable Long videoId) {
        List<VideoDownloadTask> tasks = downloadService.getVideoDownloadTasks(videoId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tasks", tasks);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 下载文件
     */
    @GetMapping("/file/{taskId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long taskId) {
        try {
            VideoDownloadTask task = downloadService.getTaskDetails(taskId);
            
            if (!"SUCCESS".equals(task.getStatus()) || task.getOutputFile() == null) {
                return ResponseEntity.notFound().build();
            }
            
            File file = new File(task.getOutputFile());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 获取文件名（已经是清理后的标题）
            String filename = file.getName();
            
            // URL 编码文件名以支持中文和特殊字符
            String encodedFilename;
            try {
                encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
                    .replaceAll("\\+", "%20");
            } catch (Exception e) {
                encodedFilename = filename;
            }
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
                
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除下载任务
     */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        try {
            downloadService.deleteDownloadTask(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Download task deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("删除下载任务失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete task: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 快速下载（使用默认设置）
     */
    @PostMapping("/quick/{videoId}")
    public ResponseEntity<?> quickDownload(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "video") String type,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            
            // 使用默认设置创建任务
            VideoDownloadTask task = downloadService.createDownloadTask(
                videoId, type, null, "best", userId
            );
            
            // 异步开始下载
            downloadService.executeDownloadAsync(task.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", task.getId());
            response.put("message", "Download started");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("快速下载失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 从请求中获取用户 ID
     */
    private Long getUserId(Authentication authentication) {
        Long userId = 0L;
        
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

