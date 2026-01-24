package com.example.finance.xiaohongshu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface XiaohongshuVideoRepository extends JpaRepository<XiaohongshuVideo, Long> {
    Optional<XiaohongshuVideo> findByVideoId(String videoId);
    List<XiaohongshuVideo> findByCreatedBy(Long userId);
    List<XiaohongshuVideo> findByStatus(String status);
    List<XiaohongshuVideo> findByCreatedByOrderByCreatedAtDesc(Long userId);
}

