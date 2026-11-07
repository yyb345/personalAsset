package com.example.finance.seo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class GuideCatalog {
    private final List<Guide> guides;

    public GuideCatalog(ObjectMapper mapper) throws IOException {
        try (var stream = new ClassPathResource("seo/guides.json").getInputStream()) {
            guides = List.copyOf(mapper.readValue(stream, new TypeReference<List<Guide>>() {}));
        }
    }

    public List<Guide> all() { return guides; }
    public Optional<Guide> find(String slug) {
        return guides.stream().filter(guide -> guide.slug().equals(slug)).findFirst();
    }
    public record Section(String heading, String body, String example) {}
    public record Guide(String slug, String title, String description, String intro,
                        List<Section> sections, String exerciseTitle, String exercise,
                        String question, String answer) {
        public String getPath() { return "/guides/" + slug; }
    }
}
