package com.example.finance.followread;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YoutubeSubtitleFailureTest {
    private final YoutubeVideoService service = spy(new YoutubeVideoService());
    private final YoutubeVideoRepository videos = mock(YoutubeVideoRepository.class);
    private final SubtitleSegmentRepository segments = mock(SubtitleSegmentRepository.class);
    private final FollowReadSentenceRepository sentences = mock(FollowReadSentenceRepository.class);
    private final YoutubeVideo video = new YoutubeVideo();

    private void setup() {
        ReflectionTestUtils.setField(service, "videoRepository", videos);
        ReflectionTestUtils.setField(service, "segmentRepository", segments);
        ReflectionTestUtils.setField(service, "sentenceRepository", sentences);
        video.setId(1L);
        video.setVideoId("n9xKblqyQ28");
        video.setTitle("Existing video");
        video.setSubtitleLanguage("en");
        when(videos.findById(1L)).thenReturn(Optional.of(video));
        when(sentences.findByYoutubeVideoId(1L)).thenReturn(List.of());
    }

    @Test void missingEnglishIsReportedWithoutDownloadingJapanese() throws Exception {
        setup();
        JsonNode metadata = new ObjectMapper().readTree("""
            {"automatic_captions":{"ja":[{"ext":"vtt","url":"https://example.com/?lang=ja"}]}}
            """);
        doReturn(metadata).when(service).readSubtitleMetadata(video);
        service.parseSubtitlesAsync(1L, "en");
        assertEquals("failed", video.getStatus());
        assertTrue(video.getErrorMessage().contains("没有可用的目标语言字幕"));
        assertEquals("en", video.getSubtitleLanguage());
        verify(service, never()).downloadSelectedSubtitles(any(), any(), any(), any());
    }

    @Test void metadataNetworkFailureIsNotReportedAsMissingEnglish() throws Exception {
        setup();
        doThrow(new IOException("network unavailable")).when(service).readSubtitleMetadata(video);
        service.parseSubtitlesAsync(1L, "en");
        assertEquals("failed", video.getStatus());
        assertTrue(video.getErrorMessage().contains("字幕信息获取失败"));
        assertFalse(video.getErrorMessage().contains("没有可用"));
        verify(service, never()).downloadSelectedSubtitles(any(), any(), any(), any());
    }

    @Test void downloadFailureDoesNotTryAnotherTrackOrLanguage() throws Exception {
        setup();
        JsonNode metadata = new ObjectMapper().readTree("""
            {"automatic_captions":{
              "en-orig":[{"ext":"vtt","url":"https://example.com/?lang=en"}],
              "en":[{"ext":"vtt","url":"https://example.com/?lang=ja&tlang=en"}],
              "ja":[{"ext":"vtt","url":"https://example.com/?lang=ja"}]}}
            """);
        doReturn(metadata).when(service).readSubtitleMetadata(video);
        doThrow(new IOException("目标语言字幕下载失败: en"))
            .when(service).downloadSelectedSubtitles(eq(video), eq(metadata), any(), eq("en"));
        service.parseSubtitlesAsync(1L, "en");
        assertEquals("failed", video.getStatus());
        assertTrue(video.getErrorMessage().contains("下载失败"));
        assertEquals("en", video.getSubtitleLanguage());
        verify(service, times(1)).downloadSelectedSubtitles(eq(video), eq(metadata),
            argThat(track -> track.key().equals("en-orig")), eq("en"));
        verify(segments, never()).save(any());
    }
}
