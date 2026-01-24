package com.example.finance.followread;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 视频下载服务
 * 实现视频解析、格式选择、下载管理、进度跟踪等功能
 */
@Service
public class VideoDownloadService {

    private static final Logger log = LoggerFactory.getLogger(VideoDownloadService.class);

    @Autowired
    private VideoDownloadTaskRepository taskRepository;

    @Autowired
    private YoutubeVideoRepository videoRepository;

    private static final String DOWNLOAD_DIR = "downloads/";
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;

    public VideoDownloadService() {
        // 确保下载目录存在
        new File(DOWNLOAD_DIR).mkdirs();
    }

    /**
     * 获取视频的可用格式列表
     */
    public List<VideoFormat> getAvailableFormats(String videoUrl) throws Exception {
        log.info("🔍 解析视频可用格式: {}", videoUrl);
        
        ProcessBuilder pb = new ProcessBuilder(
            "yt-dlp",
            "--dump-json",
            "--no-download",
            videoUrl
        );
        
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder json = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to fetch video formats");
        }

        // 解析 JSON 获取格式列表
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json.toString());
        JsonNode formatsNode = root.path("formats");
        
        List<VideoFormat> formats = new ArrayList<>();
        
        if (formatsNode.isArray()) {
            for (JsonNode formatNode : formatsNode) {
                VideoFormat format = parseFormat(formatNode);
                // 只返回有用的格式（有视频或音频）
                if (format.isHasVideo() || format.isHasAudio()) {
                    formats.add(format);
                }
            }
        }
        
        // 按质量排序
        formats.sort((a, b) -> {
            // 优先显示有视频+音频的格式
            if (a.isHasVideo() && a.isHasAudio() && !(b.isHasVideo() && b.isHasAudio())) {
                return -1;
            }
            if (b.isHasVideo() && b.isHasAudio() && !(a.isHasVideo() && a.isHasAudio())) {
                return 1;
            }
            return b.getQuality().compareTo(a.getQuality());
        });
        
        log.info("✅ 解析完成，找到 {} 个可用格式", formats.size());
        return formats;
    }

    /**
     * 解析单个格式信息
     */
    private VideoFormat parseFormat(JsonNode node) {
        VideoFormat format = new VideoFormat();
        
        format.setFormatId(node.path("format_id").asText());
        format.setExt(node.path("ext").asText());
        format.setResolution(node.path("resolution").asText("N/A"));
        format.setQuality(node.path("format_note").asText("unknown"));
        format.setFps(node.path("fps").asText());
        format.setVcodec(node.path("vcodec").asText("none"));
        format.setAcodec(node.path("acodec").asText("none"));
        
        long filesize = node.path("filesize").asLong(0);
        format.setFilesize(filesize);
        format.setFilesizeStr(formatFileSize(filesize));
        format.setNote(node.path("format").asText());
        
        // 判断是否有视频/音频
        format.setHasVideo(!"none".equals(format.getVcodec()));
        format.setHasAudio(!"none".equals(format.getAcodec()));
        
        return format;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 创建下载任务
     */
    public VideoDownloadTask createDownloadTask(Long youtubeVideoId, String downloadType, 
                                                 String formatId, String quality, Long userId) {
        Optional<YoutubeVideo> videoOpt = videoRepository.findById(youtubeVideoId);
        if (!videoOpt.isPresent()) {
            throw new RuntimeException("Video not found");
        }
        
        YoutubeVideo video = videoOpt.get();
        
        VideoDownloadTask task = new VideoDownloadTask();
        task.setYoutubeVideoId(youtubeVideoId);
        task.setVideoId(video.getVideoId());
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
            // 最佳质量（1080p+）
            return "bestvideo[height>=1080]+bestaudio/bestvideo+bestaudio/best";
        }
        
        switch (quality) {
            case "4k":
                // 4K (2160p)
                return "bestvideo[height>=2160]+bestaudio/bestvideo[height>=1440]+bestaudio/best";
            case "2k":
                // 2K (1440p)
                return "bestvideo[height>=1440][height<=2160]+bestaudio/bestvideo+bestaudio/best";
            case "1080p":
                // Full HD
                return "bestvideo[height>=1080][height<=1440]+bestaudio/bestvideo[height=1080]+bestaudio/best";
            case "720p":
                // HD
                return "bestvideo[height>=720][height<=1080]+bestaudio/bestvideo[height=720]+bestaudio/best";
            case "480p":
                // SD
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
            
            Optional<YoutubeVideo> videoOpt = videoRepository.findById(task.getYoutubeVideoId());
            if (!videoOpt.isPresent()) {
                throw new RuntimeException("Video not found");
            }
            YoutubeVideo video = videoOpt.get();
            
            // Step 2: 开始下载
            task.setStartedAt(LocalDateTime.now());
            updateTaskStatus(task, "DOWNLOADING", 10, "开始下载...");
            
            String outputPath = downloadVideo(task, video);
            
            // Step 3: 下载完成
            task.setOutputFile(outputPath);
            task.setCompletedAt(LocalDateTime.now());
            updateTaskStatus(task, "SUCCESS", 100, "下载完成！");
            
            log.info("✅ 下载任务完成: taskId={}, file={}", taskId, outputPath);
            
        } catch (Exception e) {
            log.error("❌ 下载任务失败: taskId={}", taskId, e);
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
    private String downloadVideo(VideoDownloadTask task, YoutubeVideo video) throws Exception {
        // 使用视频标题作为文件名（清理非法字符）
        String sanitizedTitle = sanitizeFilename(video.getTitle());
        String outputTemplate = DOWNLOAD_DIR + sanitizedTitle + ".%(ext)s";
        
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        
        // 根据下载类型设置参数
        if ("audio".equals(task.getDownloadType())) {
            // 仅下载音频
            command.add("-f");
            command.add("bestaudio");
            command.add("-x"); // 提取音频
            command.add("--audio-format");
            command.add("mp3");
        } else if ("video".equals(task.getDownloadType()) && task.getFormatId() != null) {
            // 下载指定格式的视频
            command.add("-f");
            command.add(task.getFormatId());
        } else {
            // 默认：根据用户选择的质量下载视频
            String quality = task.getQuality();
            String formatSelector = getFormatSelector(quality);
            
            command.add("-f");
            command.add(formatSelector);
            
            // 如果需要合并，自动选择最佳容器格式
            command.add("--merge-output-format");
            command.add("mp4");
            
            // 确保视频质量优先
            command.add("--format-sort");
            command.add("res,fps,vcodec,acodec");
            
            log.info("下载质量设置: {} -> {}", quality, formatSelector);
        }
        
        // 输出路径
        command.add("-o");
        command.add(outputTemplate);
        
        // 启用进度输出
        command.add("--newline");
        command.add("--no-warnings");
        
        // 视频URL
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
            
            // 解析进度
            Matcher matcher = progressPattern.matcher(line);
            if (matcher.find()) {
                try {
                    double percent = Double.parseDouble(matcher.group(1));
                    int progress = Math.min((int) (10 + percent * 0.9), 99); // 10-99%
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
            
            // 检查是否有文件名信息
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
        
        // 查找下载的文件（使用视频标题）
        String outputFile = findDownloadedFile(video.getTitle());
        if (outputFile == null) {
            throw new RuntimeException("Downloaded file not found");
        }
        
        log.info("✅ 文件下载完成: {}", outputFile);
        
        return outputFile;
    }

    /**
     * 清理文件名（移除非法字符）
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "video_" + System.currentTimeMillis();
        }
        
        // 移除或替换文件系统不支持的字符
        String sanitized = filename
            .replaceAll("[\\\\/:*?\"<>|]", "_")  // Windows 非法字符
            .replaceAll("[\\x00-\\x1F]", "")     // 控制字符
            .replaceAll("\\s+", " ")              // 多个空格替换为单个
            .trim();
        
        // 限制长度（保留足够空间给扩展名）
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        
        // 如果清理后为空，使用默认名称
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
        
        // 先精确匹配标题
        File[] files = dir.listFiles((d, name) -> {
            // 移除扩展名后比较
            String nameWithoutExt = name.contains(".") ? 
                name.substring(0, name.lastIndexOf('.')) : name;
            return nameWithoutExt.equals(sanitizedTitle);
        });
        
        if (files != null && files.length > 0) {
            // 返回最新的文件
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            return files[0].getPath();
        }
        
        // 如果精确匹配失败，尝试模糊匹配（以标题开头）
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
        log.info("[下载任务] ID={}, 状态={}, 进度={}%, 消息={}", task.getId(), status, progress, message);
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
        return taskRepository.findByYoutubeVideoId(videoId);
    }
}

