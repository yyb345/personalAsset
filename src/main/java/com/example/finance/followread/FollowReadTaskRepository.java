package com.example.finance.followread;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowReadTaskRepository extends JpaRepository<FollowReadTask, Long> {
    
    List<FollowReadTask> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<FollowReadTask> findBySentenceId(Long sentenceId);
    
    Optional<FollowReadTask> findByIdAndUserId(Long id, Long userId);
}

