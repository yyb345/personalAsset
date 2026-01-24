package com.example.finance.followread;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoDownloadTaskRepository extends JpaRepository<VideoDownloadTask, Long> {
    
    List<VideoDownloadTask> findByCreatedByOrderByCreatedAtDesc(Long userId);
    
    List<VideoDownloadTask> findByYoutubeVideoId(Long youtubeVideoId);
    
    List<VideoDownloadTask> findByXiaohongshuVideoId(Long xiaohongshuVideoId);
    
    Optional<VideoDownloadTask> findByIdAndCreatedBy(Long id, Long userId);
    
    List<VideoDownloadTask> findByStatus(String status);
}

