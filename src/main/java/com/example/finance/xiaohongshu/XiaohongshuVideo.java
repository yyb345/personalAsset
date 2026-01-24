package com.example.finance.xiaohongshu;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "xiaohongshu_videos")
public class XiaohongshuVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, unique = true, length = 50)
    private String videoId; // 小红书视频 ID

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl; // 完整的小红书 URL

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Integer duration; // 视频时长（秒）

    @Column(length = 100)
    private String author; // 作者名称

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl; // 视频缩略图

    @Column(nullable = false)
    private String status; // added, completed, failed

    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel; // auto, easy, medium, hard

    @Column(name = "created_by")
    private Long createdBy; // 创建者用户ID

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage; // 解析失败时的错误信息

    @Column(name = "progress_message", length = 500)
    private String progressMessage; // 解析进度信息

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "added";
        }
        if (progressMessage == null) {
            progressMessage = "已添加到视频库";
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
    }
}

