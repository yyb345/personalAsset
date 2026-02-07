package com.example.finance.xiaohongshu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class XiaohongshuVideoService {

    private static final Logger log = LoggerFactory.getLogger(XiaohongshuVideoService.class);

    @Autowired
    private XiaohongshuVideoRepository videoRepository;

    /**
     * 从小红书 URL 提取视频 ID
     */
    public String extractVideoId(String url) {
        // 支持多种小红书 URL 格式
        // https://www.xiaohongshu.com/explore/VIDEO_ID
        // https://xhslink.com/xxxxx
        
        Pattern pattern = Pattern.compile("(?:xiaohongshu\\.com/explore/|xhslink\\.com/)([a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果无法提取，使用整个 URL 的 hash 作为 ID
        return String.valueOf(url.hashCode());
    }

    /**
     * 检查 yt-dlp 是否安装（小红书也使用 yt-dlp）
     */
    public boolean isYtDlpInstalled() {
        try {
            Process process = new ProcessBuilder("yt-dlp", "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 添加视频到库（获取基本信息）
     */
    public XiaohongshuVideo addVideoToLibrary(String url, Long userId, String difficulty) {
        String videoId = extractVideoId(url);
        
        // 检查是否已经存在
        Optional<XiaohongshuVideo> existing = videoRepository.findByVideoId(videoId);
        if (existing.isPresent()) {
            return existing.get();
        }

        XiaohongshuVideo video = new XiaohongshuVideo();
        video.setVideoId(videoId);
        video.setSourceUrl(url);
        video.setStatus("added");
        video.setCreatedBy(userId);
        video.setDifficultyLevel(difficulty != null ? difficulty : "auto");
        
        // Set placeholder values (will be updated after metadata fetch)
        video.setTitle("Loading...");
        video.setDuration(0);
        video.setProgressMessage("已添加到视频库");
        
        return videoRepository.save(video);
    }

    /**
     * 异步获取视频基本信息
     */
    @Async
    @Transactional
    public void fetchVideoInfoAsync(Long videoId) {
        Optional<XiaohongshuVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            return;
        }

        XiaohongshuVideo video = videoOpt.get();
        
        try {
            // 获取视频元数据
            updateProgress(video, "正在获取视频信息...");
            fetchVideoMetadata(video);
            
            // 标记为已完成（小红书不需要解析字幕）
            video.setStatus("completed");
            video.setProgressMessage("视频信息已获取");
            video.setCompletedAt(LocalDateTime.now());
            videoRepository.save(video);
            
            log.info("✅ 小红书视频信息获取完成: videoId={}, title={}", video.getVideoId(), video.getTitle());
            
        } catch (Exception e) {
            video.setStatus("failed");
            video.setErrorMessage(e.getMessage());
            video.setProgressMessage("获取视频信息失败: " + e.getMessage());
            videoRepository.save(video);
            log.error("❌ 获取小红书视频信息失败: videoId={}", video.getVideoId(), e);
        }
    }
    
    /**
     * 更新解析进度
     */
    private void updateProgress(XiaohongshuVideo video, String message) {
        video.setProgressMessage(message);
        videoRepository.save(video);
        log.info("[小红书解析] 视频ID: {} - {}", video.getVideoId(), message);
    }

    /**
     * 获取视频元数据
     */
    private void fetchVideoMetadata(XiaohongshuVideo video) throws Exception {
        // 使用 yt-dlp 获取视频信息
        updateProgress(video, "正在连接小红书服务器...");
        ProcessBuilder pb = new ProcessBuilder(
            "yt-dlp",
            "--dump-json",
            "--no-download",
            video.getSourceUrl()
        );
        
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder json = new StringBuilder();
        String line;
        
        updateProgress(video, "正在读取视频元数据...");
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to fetch video metadata");
        }

        // 解析 JSON
        updateProgress(video, "正在解析视频信息...");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json.toString());
        
        video.setTitle(root.path("title").asText());
        video.setDescription(root.path("description").asText());
        video.setDuration(root.path("duration").asInt());
        video.setAuthor(root.path("uploader").asText());
        video.setThumbnailUrl(root.path("thumbnail").asText());
        
        videoRepository.save(video);
    }

    /**
     * 获取用户的所有视频任务
     */
    public List<XiaohongshuVideo> getUserVideos(Long userId) {
        return videoRepository.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    /**
     * 分页获取用户的视频任务
     */
    public org.springframework.data.domain.Page<XiaohongshuVideo> getUserVideosPaged(Long userId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return videoRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 获取视频详情
     */
    public Map<String, Object> getVideoDetails(Long videoId) {
        Optional<XiaohongshuVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            throw new RuntimeException("Video not found");
        }

        XiaohongshuVideo video = videoOpt.get();
        
        Map<String, Object> details = new HashMap<>();
        details.put("video", video);
        
        return details;
    }

    /**
     * 删除视频及其所有相关数据
     */
    @Transactional
    public void deleteVideo(Long videoId) {
        Optional<XiaohongshuVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            throw new RuntimeException("Video not found");
        }

        XiaohongshuVideo video = videoOpt.get();
        
        log.info("删除小红书视频及相关数据: videoId={}, title={}", video.getVideoId(), video.getTitle());
        
        // 删除视频记录
        videoRepository.delete(video);
        log.info("已删除小红书视频记录: {}", video.getVideoId());
    }
}

