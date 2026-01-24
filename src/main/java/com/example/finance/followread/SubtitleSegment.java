package com.example.finance.followread;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subtitle_segments")
public class SubtitleSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId; // 关联到 YoutubeVideo

    @Column(name = "start_time", nullable = false)
    private Double startTime; // 开始时间（秒）

    @Column(name = "end_time", nullable = false)
    private Double endTime; // 结束时间（秒）

    @Column(name = "raw_text", nullable = false, length = 2000)
    private String rawText; // 原始字幕文本

    @Column(name = "clean_text", length = 2000)
    private String cleanText; // 清洗后的文本

    @Column(name = "segment_order", nullable = false)
    private Integer segmentOrder; // 在视频中的顺序

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Double getStartTime() {
        return startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    public Double getEndTime() {
        return endTime;
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getCleanText() {
        return cleanText;
    }

    public void setCleanText(String cleanText) {
        this.cleanText = cleanText;
    }

    public Integer getSegmentOrder() {
        return segmentOrder;
    }

    public void setSegmentOrder(Integer segmentOrder) {
        this.segmentOrder = segmentOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

