package com.example.finance.followread;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 实时推送下载进度
 */
@RestController
@RequestMapping("/api/youtube/download")
@CrossOrigin(origins = "*")
public class VideoDownloadSseController {

    @Autowired
    private VideoDownloadService downloadService;

    @GetMapping(value = "/progress/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress() {
        return downloadService.createSseEmitter();
    }
}
