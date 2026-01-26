package com.example.finance.followread;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 跟读任务单词级别评分结果
 */
@Entity
@Table(name = "follow_read_task_results")
public class FollowReadTaskResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 关联的任务 ID
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    
    // 单词文本
    @Column(name = "word", length = 100, nullable = false)
    private String word;
    
    // 单词在句子中的位置
    @Column(name = "word_position")
    private Integer wordPosition;
    
    // 单词得分 (0-100)
    @Column(name = "score")
    private Integer score;
    
    // 状态: correct, incorrect
    @Column(name = "status", length = 20)
    private String status;
    
    // 反馈信息
    @Column(name = "feedback", length = 500)
    private String feedback;
    
    // 创建时间
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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Integer getWordPosition() {
        return wordPosition;
    }

    public void setWordPosition(Integer wordPosition) {
        this.wordPosition = wordPosition;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

