package com.example.finance.followread;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowReadTaskResultRepository extends JpaRepository<FollowReadTaskResult, Long> {
    
    List<FollowReadTaskResult> findByTaskIdOrderByWordPosition(Long taskId);
}

