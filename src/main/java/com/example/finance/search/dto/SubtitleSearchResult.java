package com.example.finance.search.dto;

import java.util.List;

public class SubtitleSearchResult {
    private String videoId;
    private String title;
    private String channel;
    private String language;
    private String thumbnailUrl;
    private Integer duration;
    private Double score;
    private List<String> highlights;
    private List<SegmentMatch> matchedSegments;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights;
    }

    public List<SegmentMatch> getMatchedSegments() {
        return matchedSegments;
    }

    public void setMatchedSegments(List<SegmentMatch> matchedSegments) {
        this.matchedSegments = matchedSegments;
    }
}
