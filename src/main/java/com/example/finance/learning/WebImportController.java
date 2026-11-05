package com.example.finance.learning;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/learning")
public class WebImportController {
    private final WebImportService service;
    public WebImportController(WebImportService service) { this.service = service; }
    public record Request(String url) {}
    @PostMapping("/web-import")
    public ResponseEntity<?> extract(@RequestBody Request request) {
        try { return ResponseEntity.ok(service.extract(request.url())); }
        catch (WebImportService.ImportFailure ex) {
            return ResponseEntity.status(ex.status).body(Map.of("error", ex.getMessage()));
        }
    }
}
