package com.example.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    // 查找指定分类的新闻，按发布时间降序
    List<News> findByCategoryOrderByPublishedAtDesc(String category);
    
    // 查找最近的新闻，按发布时间降序
    List<News> findTop20ByOrderByPublishedAtDesc();
    
    // 查找指定时间之后的新闻
    List<News> findByPublishedAtAfterOrderByPublishedAtDesc(LocalDateTime date);
    
    // 查找指定分类和时间之后的新闻
    List<News> findByCategoryAndPublishedAtAfterOrderByPublishedAtDesc(String category, LocalDateTime date);
}

