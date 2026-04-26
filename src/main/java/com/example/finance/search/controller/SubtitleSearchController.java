package com.example.finance.search.controller;

import com.example.finance.search.dto.*;
import com.example.finance.search.service.SubtitleSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subtitle-search")
@CrossOrigin(origins = "*")
public class SubtitleSearchController {

    @Autowired
    private SubtitleSearchService searchService;

    /**
     * 搜索字幕
     * GET /api/subtitle-search?q=关键词&page=0&size=10&language=zh
     */
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String language) {

        SubtitleSearchRequest request = new SubtitleSearchRequest();
        request.setKeyword(keyword);
        request.setPage(page);
        request.setSize(size);
        request.setLanguage(language);

        Map<String, Object> results = searchService.search(request);
        return ResponseEntity.ok(results);
    }

    /**
     * 搜索并返回精确时间点
     * GET /api/subtitle-search/locate?q=关键词&limit=20
     */
    @GetMapping("/locate")
    public ResponseEntity<?> searchWithTimestamp(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "20") int limit) {

        List<SubtitleSearchResult> results = searchService.searchWithTimestamp(keyword, limit);
        return ResponseEntity.ok(results);
    }
}
