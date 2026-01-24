package com.example.finance.xiaohongshu;

import com.example.finance.followread.VideoDownloadTask;
import com.example.finance.followread.VideoDownloadTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 小红书视频下载服务
 */
@Service
public class XiaohongshuDownloadService {

    private static final Logger log = LoggerFactory.getLogger(XiaohongshuDownloadService.class);

    @Autowired
    private VideoDownloadTaskRepository taskRepository;

    @Autowired
    private XiaohongshuVideoRepository videoRepository;

    private static final String DOWNLOAD_DIR = "downloads/";

    public XiaohongshuDownloadService() {
        // 确保下载目录存在
        new File(DOWNLOAD_DIR).mkdirs();
    }

    /**
     * 创建下载任务
     */
    public VideoDownloadTask createDownloadTask(Long xiaohongshuVideoId, String downloadType,
                                                String formatId, String quality, Long userId) {
        Optional<XiaohongshuVideo> videoOpt = videoRepository.findById(xiaohongshuVideoId);
        if (!videoOpt.isPresent()) {
            throw new RuntimeException("Video not found");
        }
        
        XiaohongshuVideo video = videoOpt.get();
        
        VideoDownloadTask task = new VideoDownloadTask();
        task.setXiaohongshuVideoId(xiaohongshuVideoId);
        task.setVideoId(video.getVideoId());
        task.setPlatform("xiaohongshu");
        task.setDownloadType(downloadType);
        task.setFormatId(formatId);
        task.setQuality(quality != null ? quality : "best");
        task.setStatus("INIT");
        task.setProgress(0);
        task.setCreatedBy(userId);
        task.setProgressMessage("等待开始下载...");
        
        return taskRepository.save(task);
    }
    
    /**
     * 根据质量选项获取格式选择器
     */
    private String getFormatSelector(String quality) {
        if (quality == null || "best".equals(quality)) {
            return "bestvideo[height>=1080]+bestaudio/bestvideo+bestaudio/best";
        }
        
        switch (quality) {
            case "4k":
                return "bestvideo[height>=2160]+bestaudio/bestvideo[height>=1440]+bestaudio/best";
            case "2k":
                return "bestvideo[height>=1440][height<=2160]+bestaudio/bestvideo+bestaudio/best";
            case "1080p":
                return "bestvideo[height>=1080][height<=1440]+bestaudio/bestvideo[height=1080]+bestaudio/best";
            case "720p":
                return "bestvideo[height>=720][height<=1080]+bestaudio/bestvideo[height=720]+bestaudio/best";
            case "480p":
                return "bestvideo[height>=480][height<=720]+bestaudio/bestvideo[height=480]+bestaudio/best";
            default:
                return "bestvideo+bestaudio/best";
        }
    }

    /**
     * 异步执行下载任务
     */
    @Async
    @Transactional
    public void executeDownloadAsync(Long taskId) {
        Optional<VideoDownloadTask> taskOpt = taskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            return;
        }

        VideoDownloadTask task = taskOpt.get();
        
        try {
            // Step 1: 准备下载
            updateTaskStatus(task, "PARSING", 5, "正在解析下载链接...");
            
            Optional<XiaohongshuVideo> videoOpt = videoRepository.findById(task.getXiaohongshuVideoId());
            if (!videoOpt.isPresent()) {
                throw new RuntimeException("Video not found");
            }
            XiaohongshuVideo video = videoOpt.get();
            
            // Step 2: 开始下载
            task.setStartedAt(LocalDateTime.now());
            updateTaskStatus(task, "DOWNLOADING", 10, "开始下载...");
            
            String outputPath = downloadVideo(task, video);
            
            // Step 3: 下载完成
            task.setOutputFile(outputPath);
            task.setCompletedAt(LocalDateTime.now());
            updateTaskStatus(task, "SUCCESS", 100, "下载完成！");
            
            log.info("✅ 小红书下载任务完成: taskId={}, file={}", taskId, outputPath);
            
        } catch (Exception e) {
            log.error("❌ 小红书下载任务失败: taskId={}", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setProgressMessage("下载失败: " + e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }

    /**
     * 执行视频下载
     */
    private String downloadVideo(VideoDownloadTask task, XiaohongshuVideo video) throws Exception {
        // 使用视频标题作为文件名
        String sanitizedTitle = sanitizeFilename(video.getTitle());
        String outputTemplate = DOWNLOAD_DIR + sanitizedTitle + ".%(ext)s";
        
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        
        // 根据下载类型设置参数
        if ("audio".equals(task.getDownloadType())) {
            command.add("-f");
            command.add("bestaudio");
            command.add("-x");
            command.add("--audio-format");
            command.add("mp3");
        } else if ("video".equals(task.getDownloadType()) && task.getFormatId() != null) {
            command.add("-f");
            command.add(task.getFormatId());
        } else {
            String quality = task.getQuality();
            String formatSelector = getFormatSelector(quality);
            
            command.add("-f");
            command.add(formatSelector);
            command.add("--merge-output-format");
            command.add("mp4");
            command.add("--format-sort");
            command.add("res,fps,vcodec,acodec");
            
            log.info("下载质量设置: {} -> {}", quality, formatSelector);
        }
        
        command.add("-o");
        command.add(outputTemplate);
        command.add("--newline");
        command.add("--no-warnings");
        command.add(video.getSourceUrl());
        
        log.info("执行下载命令: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // 读取输出并更新进度
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        Pattern progressPattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.?\\d*)%.*?at\\s+([\\d\\.]+\\w+/s)?");
        
        while ((line = reader.readLine()) != null) {
            log.debug("yt-dlp: {}", line);
            
            Matcher matcher = progressPattern.matcher(line);
            if (matcher.find()) {
                try {
                    double percent = Double.parseDouble(matcher.group(1));
                    int progress = Math.min((int) (10 + percent * 0.9), 99);
                    String speed = matcher.group(2);
                    
                    task.setProgress(progress);
                    task.setProgressMessage("下载中... " + String.format("%.1f", percent) + "%");
                    if (speed != null) {
                        task.setDownloadSpeed(speed);
                    }
                    taskRepository.save(task);
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
            
            if (line.contains("[download] Destination:")) {
                String filename = line.substring(line.indexOf("Destination:") + 12).trim();
                task.setOutputFile(filename);
                taskRepository.save(task);
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("Download failed with exit code: " + exitCode);
        }
        
        // 查找下载的文件
        String outputFile = findDownloadedFile(video.getTitle());
        if (outputFile == null) {
            throw new RuntimeException("Downloaded file not found");
        }
        
        log.info("✅ 文件下载完成: {}", outputFile);
        
        return outputFile;
    }

    /**
     * 清理文件名
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "video_" + System.currentTimeMillis();
        }
        
        String sanitized = filename
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("[\\x00-\\x1F]", "")
            .replaceAll("\\s+", " ")
            .trim();
        
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        
        if (sanitized.isEmpty()) {
            sanitized = "video_" + System.currentTimeMillis();
        }
        
        return sanitized;
    }

    /**
     * 查找下载的文件
     */
    private String findDownloadedFile(String videoTitle) {
        File dir = new File(DOWNLOAD_DIR);
        String sanitizedTitle = sanitizeFilename(videoTitle);
        
        File[] files = dir.listFiles((d, name) -> {
            String nameWithoutExt = name.contains(".") ? 
                name.substring(0, name.lastIndexOf('.')) : name;
            return nameWithoutExt.equals(sanitizedTitle);
        });
        
        if (files != null && files.length > 0) {
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            return files[0].getPath();
        }
        
        files = dir.listFiles((d, name) -> name.startsWith(sanitizedTitle));
        
        if (files != null && files.length > 0) {
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            return files[0].getPath();
        }
        
        return null;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(VideoDownloadTask task, String status, int progress, String message) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setProgressMessage(message);
        taskRepository.save(task);
        log.info("[小红书下载] ID={}, 状态={}, 进度={}%, 消息={}", task.getId(), status, progress, message);
    }

    /**
     * 获取用户的下载任务列表
     */
    public List<VideoDownloadTask> getUserDownloadTasks(Long userId) {
        return taskRepository.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取任务详情
     */
    public VideoDownloadTask getTaskDetails(Long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    /**
     * 删除下载任务及文件
     */
    @Transactional
    public void deleteDownloadTask(Long taskId) {
        Optional<VideoDownloadTask> taskOpt = taskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("Task not found");
        }
        
        VideoDownloadTask task = taskOpt.get();
        
        // 删除下载的文件
        if (task.getOutputFile() != null) {
            try {
                Path filePath = Paths.get(task.getOutputFile());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("已删除下载文件: {}", task.getOutputFile());
                }
            } catch (Exception e) {
                log.warn("删除下载文件失败: {}", e.getMessage());
            }
        }
        
        // 删除任务记录
        taskRepository.delete(task);
        log.info("已删除下载任务: taskId={}", taskId);
    }

    /**
     * 获取视频的所有下载任务
     */
    public List<VideoDownloadTask> getVideoDownloadTasks(Long videoId) {
        return taskRepository.findByXiaohongshuVideoId(videoId);
    }
}

