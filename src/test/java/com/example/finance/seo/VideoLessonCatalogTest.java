package com.example.finance.seo;

import com.example.finance.followread.SubtitleSegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class VideoLessonCatalogTest {
    @Test void editorialLessonsHaveWorkingGuideLinksAndAuditedVideoIds() throws Exception {
        var mapper = new ObjectMapper();
        var catalog = new VideoLessonCatalog(mapper);
        var guides = new GuideCatalog(mapper);
        assertThat(catalog.all()).extracting(VideoLessonCatalog.Lesson::videoId)
            .containsExactly("AVQzG2MY858", "Lg-meK5IU8Q", "LCEmiRjPEtQ", "rtuKId76DZI", "sW9npZVpiMI");
        for (var lesson : catalog.all()) {
            assertThat(lesson.overview()).isNotBlank();
            assertThat(lesson.terms()).hasSizeGreaterThanOrEqualTo(3);
            assertThat(lesson.steps()).hasSize(3);
            for (var slug : lesson.guideSlugs()) assertThat(guides.find(slug)).isPresent();
            for (var term : lesson.terms()) {
                assertThat(term.seconds()).isGreaterThanOrEqualTo(0);
                assertThat(term.getTimestamp()).matches("\\d{2}:\\d{2}");
            }
        }
    }

    @Test void notesRequireMatchingActualCaptionTimesAndExpressions() throws Exception {
        var catalog = new VideoLessonCatalog(new ObjectMapper());
        // Short observed caption excerpts from the public IBM Technology transcript, audited 2026-11-08.
        var subtitles = List.of(caption(36.9, "They lack procedural knowledge."),
            caption(162.4, "tells agent exactly when this skill applies"),
            caption(162.4, "trigger condition"), caption(274.6, "progressive disclosure"));
        // The trigger phrase is deliberately in the wrong caption: incomplete evidence must not render notes.
        assertThat(catalog.matching("Lg-meK5IU8Q", subtitles)).isEmpty();
        var corrected = List.of(caption(36.9, "They lack procedural knowledge."),
            caption(155.1, "trigger condition"), caption(274.6, "progressive disclosure"));
        assertThat(catalog.matching("Lg-meK5IU8Q", corrected)).isPresent();
        assertThat(catalog.matching("Lg-meK5IU8Q", List.of())).isEmpty();
        assertThat(catalog.matching("unreviewed", corrected)).isEmpty();
    }

    private SubtitleSegment caption(double seconds, String text) {
        var segment = new SubtitleSegment(); segment.setStartTime(seconds); segment.setRawText(text); return segment;
    }
}
