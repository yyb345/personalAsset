package com.example.finance.followread;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YoutubeVideoRepository extends JpaRepository<YoutubeVideo, Long> {
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update YoutubeVideo v set v.aiSummary = :summary where v.id = :id")
    int updateAiSummary(@org.springframework.data.repository.query.Param("id") Long id,
                        @org.springframework.data.repository.query.Param("summary") String summary);

    Optional<YoutubeVideo> findByVideoId(String videoId);
    List<YoutubeVideo> findByCreatedBy(Long userId);
    List<YoutubeVideo> findByStatus(String status);
    List<YoutubeVideo> findByCreatedByOrderByCreatedAtDesc(Long userId);
    List<YoutubeVideo> findAllByOrderByCreatedAtDesc();

    // 分页查询
    Page<YoutubeVideo> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<YoutubeVideo> findByCreatedByOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 置顶优先排序的分页查询
    Page<YoutubeVideo> findAllByOrderByPinnedDescPinnedAtDescCreatedAtDesc(Pageable pageable);
}

