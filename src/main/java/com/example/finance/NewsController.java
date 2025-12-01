package com.example.finance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    
    @Autowired
    private NewsService newsService;
    
    /**
     * 获取热门新闻
     */
    @GetMapping("/hot")
    public ResponseEntity<Map<String, Object>> getHotNews() {
        try {
            List<News> news = newsService.getHotNews();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", news);
            response.put("total", news.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取新闻失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 按分类获取新闻
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getNewsByCategory(@PathVariable String category) {
        try {
            List<News> news = newsService.getNewsByCategory(category);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", news);
            response.put("total", news.size());
            response.put("category", category);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取分类新闻失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 刷新新闻缓存
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshNews() {
        try {
            newsService.refreshNews();
            List<News> news = newsService.getHotNews();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "新闻已刷新");
            response.put("total", news.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "刷新新闻失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}

