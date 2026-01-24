package com.example.finance.followread;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FollowReadSentenceRepository extends JpaRepository<FollowReadSentence, Long> {
    List<FollowReadSentence> findByDifficulty(String difficulty);
    List<FollowReadSentence> findByCategory(String category);
    List<FollowReadSentence> findByYoutubeVideoId(Long youtubeVideoId);
    List<FollowReadSentence> findByYoutubeVideoIdOrderBySentenceOrder(Long youtubeVideoId);
}

