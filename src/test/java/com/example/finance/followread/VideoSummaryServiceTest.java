package com.example.finance.followread;

import com.example.finance.ai.LanguageModelClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoSummaryServiceTest {
    private final YoutubeVideoRepository videos = mock(YoutubeVideoRepository.class);
    private final SubtitleSegmentRepository subtitles = mock(SubtitleSegmentRepository.class);
    private final LanguageModelClient model = mock(LanguageModelClient.class);
    private final VideoSummaryService service = new VideoSummaryService(videos, subtitles, model);

    private YoutubeVideo video() {
        YoutubeVideo video = new YoutubeVideo();
        video.setId(1L);
        video.setStatus("completed");
        when(videos.findByVideoId("abc")).thenReturn(Optional.of(video));
        return video;
    }

    @Test void cachedSummaryNeverCallsModel() {
        video().setAiSummary("已保存的摘要");
        assertTrue(service.summarize("abc").cached());
        assertEquals("已保存的摘要", service.summarize("abc").summary());
        verifyNoInteractions(model, subtitles);
    }

    @Test void includesAllSubtitleChunksAndPersistsFinalSummary() {
        video();
        SubtitleSegment segment = new SubtitleSegment();
        segment.setRawText("a".repeat(12000) + "最后的字幕");
        when(subtitles.findByVideoIdOrderBySegmentOrder(1L)).thenReturn(List.of(segment));
        when(model.complete(anyString(), anyString())).thenReturn("分段一", "分段二", "最终摘要");
        assertEquals("最终摘要", service.summarize("abc").summary());
        verify(model).complete(anyString(), eq("最后的字幕\n"));
        verify(model).complete(anyString(), eq("分段一\n\n分段二"));
        verify(videos).updateAiSummary(1L, "最终摘要");
    }

    @Test void emptySubtitlesDoNotCallModel() {
        video();
        when(subtitles.findByVideoIdOrderBySegmentOrder(1L)).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> service.summarize("abc"));
        verifyNoInteractions(model);
    }

    @Test void failedGenerationIsNotCached() {
        video();
        SubtitleSegment segment = new SubtitleSegment();
        segment.setRawText("字幕");
        when(subtitles.findByVideoIdOrderBySegmentOrder(1L)).thenReturn(List.of(segment));
        when(model.complete(anyString(), anyString())).thenThrow(new RuntimeException("unavailable"));
        assertThrows(RuntimeException.class, () -> service.summarize("abc"));
        verify(videos, never()).updateAiSummary(anyLong(), anyString());
    }
}
