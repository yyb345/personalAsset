package com.example.finance.followread;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_read_sentences")
public class FollowReadSentence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false, length = 2000)
    private String phonetic; // IPA音标

    @Column(length = 500)
    private String audioUrl; // 标准音频URL（YouTube句子可以为空，使用视频片段）

    @Column(nullable = false)
    private String difficulty; // easy, medium, hard

    @Column(length = 100)
    private String category; // VOA, TED, Daily

    // YouTube 视频关联字段
    @Column(name = "youtube_video_id")
    private Long youtubeVideoId; // 关联到 YoutubeVideo 表

    @Column(name = "start_time")
    private Double startTime; // 在视频中的开始时间（秒）

    @Column(name = "end_time")
    private Double endTime; // 在视频中的结束时间（秒）

    @Column(name = "sentence_order")
    private Integer sentenceOrder; // 在视频中的顺序

    @Column(name = "video_url", length = 500)
    private String videoUrl; // YouTube 视频 URL（可选）

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public void setYoutubeVideoId(Long youtubeVideoId) {
        this.youtubeVideoId = youtubeVideoId;
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

    public Integer getSentenceOrder() {
        return sentenceOrder;
    }

    public void setSentenceOrder(Integer sentenceOrder) {
        this.sentenceOrder = sentenceOrder;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}

