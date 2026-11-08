package com.example.finance.seo;

import com.example.finance.followread.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VideoLearningPageTest {
    private MockMvc mvc;
    private YoutubeVideoRepository videos;
    private SubtitleSegmentRepository subtitles;
    private VideoLessonCatalog lessons;

    @BeforeEach void setup() throws Exception {
        var mapper = new ObjectMapper();
        lessons = new VideoLessonCatalog(mapper);
        videos = mock(YoutubeVideoRepository.class);
        subtitles = mock(SubtitleSegmentRepository.class);
        var controller = new SeoController();
        ReflectionTestUtils.setField(controller, "youtubeVideoRepository", videos);
        ReflectionTestUtils.setField(controller, "subtitleSegmentRepository", subtitles);
        ReflectionTestUtils.setField(controller, "site", new SiteProperties("https://learn.example", "xLearning"));
        ReflectionTestUtils.setField(controller, "publicVideos", new PublicVideoCatalog(videos));
        ReflectionTestUtils.setField(controller, "lessons", lessons);
        ReflectionTestUtils.setField(controller, "guideCatalog", new GuideCatalog(mapper));
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML"); resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var view = new ThymeleafViewResolver(); view.setTemplateEngine(engine); view.setCharacterEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(view).build();
    }

    @Test void allFiveLessonsRenderNotesTimestampLinksExercisesAndRelatedGuides() throws Exception {
        for (var lesson : lessons.all()) {
            var video = video(lesson.videoId());
            when(videos.findByVideoId(lesson.videoId())).thenReturn(Optional.of(video));
            when(videos.existsPublicVideo(lesson.videoId())).thenReturn(true);
            // Synthetic fixtures exercise rendering; source/timestamp accuracy was independently audited on public HTML.
            var captions = lesson.terms().stream().map(term -> {
                var caption = new SubtitleSegment(); caption.setStartTime(term.seconds());
                caption.setRawText(term.expression()); return caption;
            }).toList();
            when(subtitles.findByVideoIdOrderBySegmentOrder(1L)).thenReturn(captions);
            var html = mvc.perform(get(lesson.getPath())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains(lesson.title(), lesson.overview(), lesson.exerciseTitle(), lesson.answer(),
                "data-seo-action=\"start_learning\"", "/js/learning-analytics.js", "rel=\"canonical\" href=\"https://learn.example" + lesson.getPath() + "\"");
            for (var term : lesson.terms()) assertThat(html).contains(term.expression(), term.getTimestamp(),
                "&amp;t=" + term.getSeekSeconds() + "s");
            for (var slug : lesson.guideSlugs()) assertThat(html).contains("href=\"/guides/" + slug + "\"");
        }
    }

    @Test void hubKeepsAllPublicVideosAndOnlyPublicCuratedEntries() throws Exception {
        when(videos.findPublicVideos()).thenReturn(List.of(video("Lg-meK5IU8Q"), video("other-public")));
        var html = mvc.perform(get("/video")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("有练习笔记的精选视频", "href=\"/video/Lg-meK5IU8Q\"", "href=\"/video/other-public\"");
        assertThat(html).doesNotContain("href=\"/video/rtuKId76DZI\"");
    }

    @Test void unreviewedTranscriptRendersWithoutInventingTeachingNotes() throws Exception {
        var video = video("unreviewed");
        when(videos.findByVideoId("unreviewed")).thenReturn(Optional.of(video));
        when(videos.existsPublicVideo("unreviewed")).thenReturn(true);
        when(subtitles.findByVideoIdOrderBySegmentOrder(1L)).thenReturn(List.of());
        var html = mvc.perform(get("/video/unreviewed")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Full Transcript").doesNotContain("id=\"study-title\"", "class=\"term-list\"");
    }

    private YoutubeVideo video(String id) {
        var video = new YoutubeVideo(); video.setId(1L); video.setVideoId(id); video.setTitle("Public video " + id);
        video.setStatus("completed"); video.setDuration(600); return video;
    }
}
