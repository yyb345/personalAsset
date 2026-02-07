package com.example.finance.followread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跟读练习任务 API 控制器
 */
@RestController
@RequestMapping("/api/follow-read/tasks")
@CrossOrigin(origins = "*")
public class FollowReadTaskController {

    private static final Logger log = LoggerFactory.getLogger(FollowReadTaskController.class);

    @Autowired
    private FollowReadTaskService taskService;

    /**
     * 创建跟读任务
     * POST /api/follow-read/tasks
     */
    @PostMapping
    public ResponseEntity<?> createTask(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            Long sentenceId = Long.valueOf(request.get("sentenceId").toString());
            Long userId = getUserId(authentication);
            
            FollowReadTask task = taskService.createTask(sentenceId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", task.getId());
            response.put("sentenceId", task.getSentenceId());
            response.put("status", task.getStatus());
            response.put("createdAt", task.getCreatedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建任务失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create task: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 提交录音
     * POST /api/follow-read/tasks/{taskId}/submit
     */
    @PostMapping("/{taskId}/submit")
    public ResponseEntity<?> submitRecording(
            @PathVariable Long taskId,
            @RequestParam("audio") MultipartFile audioFile,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            
            FollowReadTask task = taskService.submitRecording(taskId, audioFile, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", task.getId());
            response.put("status", task.getStatus());
            response.put("message", "Recording submitted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("提交录音失败: taskId={}", taskId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to submit recording: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取任务详情和评分结果
     * GET /api/follow-read/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskDetails(
            @PathVariable Long taskId,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            
            FollowReadTask task = taskService.getTaskDetails(taskId, userId);
            List<FollowReadTaskResult> results = taskService.getTaskResults(taskId);
            
            // 构建任务响应
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("id", task.getId());
            taskData.put("sentenceId", task.getSentenceId());
            taskData.put("status", task.getStatus());
            taskData.put("overallScore", task.getOverallScore());
            taskData.put("pronunciationScore", task.getPronunciationScore());
            taskData.put("fluencyScore", task.getFluencyScore());
            taskData.put("intonationScore", task.getIntonationScore());
            taskData.put("audioUrl", task.getAudioUrl());
            taskData.put("createdAt", task.getCreatedAt());
            taskData.put("submittedAt", task.getSubmittedAt());
            taskData.put("completedAt", task.getCompletedAt());
            taskData.put("errorMessage", task.getErrorMessage());
            
            // 构建结果响应
            List<Map<String, Object>> resultData = new java.util.ArrayList<>();
            for (FollowReadTaskResult result : results) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("word", result.getWord());
                resultMap.put("wordPosition", result.getWordPosition());
                resultMap.put("score", result.getScore());
                resultMap.put("status", result.getStatus());
                resultMap.put("feedback", result.getFeedback());
                resultData.add(resultMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", taskData);
            response.put("results", resultData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取任务详情失败: taskId={}", taskId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get task details: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 从认证信息中获取用户 ID
     */
    private Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // 如果没有认证，返回默认用户ID（用于开发测试）
            return 1L;
        }
        
        // 这里需要根据你的认证实现来获取用户ID
        // 假设用户信息存储在 authentication.getPrincipal() 中
        try {
            // 如果使用 Spring Security 的 UserDetails
            if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                // 从 UserDetails 中提取用户ID
                // 这里需要根据你的实际实现来调整
                return 1L; // 临时返回默认值
            }
            
            // 如果 principal 是用户名，可能需要查询数据库获取用户ID
            // 这里简化处理，返回默认值
            return 1L;
        } catch (Exception e) {
            log.warn("无法获取用户ID，使用默认值", e);
            return 1L;
        }
    }
}

