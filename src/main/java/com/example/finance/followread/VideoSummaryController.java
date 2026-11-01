package com.example.finance.followread;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api/youtube")
public class VideoSummaryController {
    private final VideoSummaryService summaries;

    public VideoSummaryController(VideoSummaryService summaries) { this.summaries = summaries; }

    @PostMapping("/summary/{videoId}")
    public ResponseEntity<?> summarize(@PathVariable String videoId) {
        try {
            return ResponseEntity.ok(summaries.summarize(videoId));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason()));
        }
    }
}
