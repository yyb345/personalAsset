package com.example.finance.followread;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 跟读练习任务实体
 */
@Entity
@Table(name = "follow_read_tasks")
public class FollowReadTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 关联的句子 ID
    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;
    
    // 创建者用户 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    // 任务状态: pending, processing, completed, failed
    @Column(name = "status")
    private String status;
    
    // 用户录音文件路径
    @Column(name = "audio_url", length = 500)
    private String audioUrl;
    
    // 总体得分 (0-100)
    @Column(name = "overall_score")
    private Integer overallScore;
    
    // 发音得分 (0-100)
    @Column(name = "pronunciation_score")
    private Integer pronunciationScore;
    
    // 流利度得分 (0-100)
    @Column(name = "fluency_score")
    private Integer fluencyScore;
    
    // 语调得分 (0-100)
    @Column(name = "intonation_score")
    private Integer intonationScore;
    
    // 错误信息
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    // 创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 提交时间（录音上传时间）
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    // 完成时间
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "pending";
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(Long sentenceId) {
        this.sentenceId = sentenceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public Integer getPronunciationScore() {
        return pronunciationScore;
    }

    public void setPronunciationScore(Integer pronunciationScore) {
        this.pronunciationScore = pronunciationScore;
    }

    public Integer getFluencyScore() {
        return fluencyScore;
    }

    public void setFluencyScore(Integer fluencyScore) {
        this.fluencyScore = fluencyScore;
    }

    public Integer getIntonationScore() {
        return intonationScore;
    }

    public void setIntonationScore(Integer intonationScore) {
        this.intonationScore = intonationScore;
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

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}

