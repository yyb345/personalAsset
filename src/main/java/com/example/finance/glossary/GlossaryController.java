package com.example.finance.glossary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/glossary")
public class GlossaryController {
    private final GlossaryService service;
    public GlossaryController(GlossaryService service) { this.service = service; }
    public record HighlightRequest(@NotEmpty @Size(max=100) List<@NotNull @Size(max=10000) String> texts,
                                   @Pattern(regexp="zh|en") String language) {}
    @PostMapping("/highlight")
    public GlossaryService.HighlightResponse highlight(@Valid @RequestBody HighlightRequest request) {
        return service.highlight(request.texts());
    }
    @GetMapping("/status")
    public java.util.Map<String, Object> status() { return service.status(); }
}
