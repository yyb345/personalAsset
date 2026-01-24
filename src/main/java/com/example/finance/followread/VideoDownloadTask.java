package com.example.finance.followread;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 视频下载任务实体
 */
@Entity
@Table(name = "video_download_tasks")
public class VideoDownloadTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 关联的 YouTube 视频
    @Column(name = "youtube_video_id")
    private Long youtubeVideoId;
    
    // 视频 ID（用于文件命名）
    @Column(name = "video_id")
    private String videoId;
    
    // 下载类型: video, audio, video_audio
    @Column(name = "download_type")
    private String downloadType;
    
    // 选择的格式 ID（yt-dlp format ID）
    @Column(name = "format_id")
    private String formatId;
    
    // 质量描述（如 1080p, 720p）
    @Column(name = "quality")
    private String quality;
    
    // 任务状态: INIT, PARSING, DOWNLOADING, MERGING, SUCCESS, FAILED
    @Column(name = "status")
    private String status;
    
    // 进度百分比 (0-100)
    @Column(name = "progress")
    private Integer progress;
    
    // 进度消息
    @Column(name = "progress_message", length = 500)
    private String progressMessage;
    
    // 下载速度（如 "1.5MB/s"）
    @Column(name = "download_speed")
    private String downloadSpeed;
    
    // 已下载大小（字节）
    @Column(name = "downloaded_bytes")
    private Long downloadedBytes;
    
    // 总大小（字节）
    @Column(name = "total_bytes")
    private Long totalBytes;
    
    // 输出文件路径
    @Column(name = "output_file")
    private String outputFile;
    
    // 错误信息
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    // 创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 开始时间
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    // 完成时间
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // 创建者 ID
    @Column(name = "created_by")
    private Long createdBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "INIT";
        }
        if (progress == null) {
            progress = 0;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public void setYoutubeVideoId(Long youtubeVideoId) {
        this.youtubeVideoId = youtubeVideoId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(String downloadType) {
        this.downloadType = downloadType;
    }

    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public String getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public Long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(Long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}

