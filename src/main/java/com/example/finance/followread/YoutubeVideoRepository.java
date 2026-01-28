package com.example.finance.followread;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YoutubeVideoRepository extends JpaRepository<YoutubeVideo, Long> {
    Optional<YoutubeVideo> findByVideoId(String videoId);
    List<YoutubeVideo> findByCreatedBy(Long userId);
    List<YoutubeVideo> findByStatus(String status);
    List<YoutubeVideo> findByCreatedByOrderByCreatedAtDesc(Long userId);
    List<YoutubeVideo> findAllByOrderByCreatedAtDesc();
}

