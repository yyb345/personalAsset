package com.example.finance.followread;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtitleSegmentRepository extends JpaRepository<SubtitleSegment, Long> {
    List<SubtitleSegment> findByVideoIdOrderBySegmentOrder(Long videoId);
    void deleteByVideoId(Long videoId);
}

