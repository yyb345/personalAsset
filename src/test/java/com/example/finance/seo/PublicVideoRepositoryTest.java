package com.example.finance.seo;

import com.example.finance.followread.SubtitleSegment;
import com.example.finance.followread.SubtitleSegmentRepository;
import com.example.finance.followread.YoutubeVideo;
import com.example.finance.followread.YoutubeVideoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.config.import="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PublicVideoRepositoryTest {
    @Autowired YoutubeVideoRepository videos;
    @Autowired SubtitleSegmentRepository subtitles;

    @Test void onlyCompletedVideosWithReadableCaptionsArePublic() {
        var valid = video("valid", "completed");
        var blank = video("blank", "completed");
        var processing = video("processing", "parsing");
        video("missing", "completed");
        caption(valid, "Original caption", "  ");
        caption(blank, "  ", "  ");
        caption(processing, "Not ready yet", null);
        assertThat(videos.findPublicVideos()).extracting(YoutubeVideo::getVideoId).containsExactly("valid");
        assertThat(videos.existsPublicVideo("valid")).isTrue();
        for (String id : new String[]{"blank", "processing", "missing", "unknown"}) {
            assertThat(videos.existsPublicVideo(id)).isFalse();
        }
    }

    private YoutubeVideo video(String id, String status) {
        var video = new YoutubeVideo();
        video.setVideoId(id); video.setTitle(id); video.setDuration(60);
        video.setSourceUrl("https://www.youtube.com/watch?v=" + id); video.setStatus(status);
        return videos.saveAndFlush(video);
    }

    private void caption(YoutubeVideo video, String raw, String clean) {
        var segment = new SubtitleSegment();
        segment.setVideoId(video.getId()); segment.setStartTime(0.0); segment.setEndTime(1.0);
        segment.setSegmentOrder(0); segment.setRawText(raw); segment.setCleanText(clean);
        subtitles.saveAndFlush(segment);
        if (!raw.isBlank() && (clean == null || clean.isBlank())) {
            assertThat(segment.getDisplayText()).isEqualTo(raw);
        }
    }
}
