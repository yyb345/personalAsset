package com.example.finance.seo;

import com.example.finance.followread.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeoControllerTest {
    @Test void transcriptUsesConfiguredOriginAndEscapesScriptContent() throws Exception {
        var repository = mock(YoutubeVideoRepository.class);
        var subtitles = mock(SubtitleSegmentRepository.class);
        var video = new YoutubeVideo();
        video.setVideoId("abc123"); video.setTitle("</script><script>alert(1)</script>"); video.setStatus("completed");
        when(repository.findByVideoId("abc123")).thenReturn(Optional.of(video));
        when(repository.existsPublicVideo("abc123")).thenReturn(true);
        when(subtitles.findByVideoIdOrderBySegmentOrder(null)).thenReturn(List.of());
        var controller = new SeoController();
        ReflectionTestUtils.setField(controller, "youtubeVideoRepository", repository);
        ReflectionTestUtils.setField(controller, "subtitleSegmentRepository", subtitles);
        ReflectionTestUtils.setField(controller, "site", new SiteProperties("https://example.com/", "Example"));
        ReflectionTestUtils.setField(controller, "publicVideos", new PublicVideoCatalog(repository));
        ReflectionTestUtils.setField(controller, "lessons", new VideoLessonCatalog(new ObjectMapper()));
        ReflectionTestUtils.setField(controller, "guideCatalog", new GuideCatalog(new ObjectMapper()));
        var model = new ExtendedModelMap();
        assertThat(controller.videoPage("abc123", model, new MockHttpServletResponse())).isEqualTo("seo/video");
        assertThat(model.get("pageUrl")).isEqualTo("https://example.com/video/abc123");
        String json = (String) model.get("jsonLd");
        assertThat(json).doesNotContain("</script>", "uploadDate", "contentUrl", "VideoObject");
        assertThat(new ObjectMapper().readTree(json).get("@graph").get(1).get("name").asText()).isEqualTo(video.getTitle());
        verify(repository, never()).findPublicVideos();
    }

    @Test void completedVideoWithoutPublicTranscriptReturns404() {
        var repository = mock(YoutubeVideoRepository.class);
        var video = new YoutubeVideo(); video.setStatus("completed");
        when(repository.findByVideoId("empty")).thenReturn(Optional.of(video));
        var controller = new SeoController();
        ReflectionTestUtils.setField(controller, "youtubeVideoRepository", repository);
        ReflectionTestUtils.setField(controller, "publicVideos", new PublicVideoCatalog(repository));
        var response = new MockHttpServletResponse();
        assertThat(controller.videoPage("empty", new ExtendedModelMap(), response)).isEqualTo("seo/not-found");
        assertThat(response.getStatus()).isEqualTo(404);
    }
}
