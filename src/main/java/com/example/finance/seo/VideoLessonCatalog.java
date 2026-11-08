package com.example.finance.seo;

import com.example.finance.followread.SubtitleSegment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Human-authored study notes. No user videos or database records are published by this catalog. */
@Component
public class VideoLessonCatalog {
    private final List<Lesson> lessons;

    public VideoLessonCatalog(ObjectMapper mapper) throws IOException {
        try (var stream = new ClassPathResource("seo/video-lessons.json").getInputStream()) {
            lessons = List.copyOf(mapper.readValue(stream, new TypeReference<List<Lesson>>() {}));
        }
        if (lessons.stream().map(Lesson::videoId).distinct().count() != lessons.size()) {
            throw new IllegalArgumentException("Duplicate video lesson IDs");
        }
    }

    public List<Lesson> all() { return lessons; }
    public Optional<Lesson> find(String videoId) {
        return lessons.stream().filter(lesson -> lesson.videoId().equals(videoId)).findFirst();
    }

    /** Hide notes when captions have changed or are absent, instead of showing ungrounded timestamps. */
    public Optional<Lesson> matching(String videoId, List<SubtitleSegment> subtitles) {
        return find(videoId).filter(lesson -> lesson.terms().stream().allMatch(term -> subtitles.stream().anyMatch(segment ->
            segment.getStartTime() != null && Math.abs(segment.getStartTime() - term.seconds()) <= 2
                && normalize(segment.getDisplayText()).contains(normalize(term.expression())))));
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    public record Term(double seconds, String expression, String explanation) {
        public String getTimestamp() {
            int seconds = (int) this.seconds;
            return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
        }
        public int getSeekSeconds() { return (int) seconds; }
    }
    public record Lesson(String videoId, String title, String topic, String overview, String focus,
                         List<Term> terms, String exerciseTitle, List<String> steps, String answer,
                         List<String> guideSlugs, String reviewedOn) {
        public String getPath() { return "/video/" + videoId; }
    }
}
