package com.example.finance.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.*;
import com.example.finance.followread.SubtitleSegment;
import com.example.finance.followread.YoutubeVideo;
import com.example.finance.search.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubtitleSearchService {

    private static final Logger log = LoggerFactory.getLogger(SubtitleSearchService.class);
    private static final String INDEX_NAME = "youtube_subtitles";

    @Autowired(required = false)
    private ElasticsearchClient esClient;

    @Value("${elasticsearch.enabled:true}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled && esClient != null) {
            initIndex();
        } else {
            log.info("⚠️ Elasticsearch 未启用或未配置");
        }
    }

    /**
     * 检查 ES 是否可用
     */
    private boolean isAvailable() {
        return enabled && esClient != null;
    }

    /**
     * 索引视频字幕（解析完成后调用）
     */
    public void indexVideo(YoutubeVideo video, List<SubtitleSegment> segments) {
        if (!isAvailable()) {
            log.debug("ES 未启用，跳过索引");
            return;
        }

        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("video_id", video.getVideoId());
            doc.put("title", video.getTitle());
            doc.put("channel", video.getChannel());
            doc.put("language", video.getSubtitleLanguage());
            doc.put("thumbnail_url", video.getThumbnailUrl());
            doc.put("duration", video.getDuration());

            // 字幕片段
            List<Map<String, Object>> segmentList = segments.stream()
                .map(s -> {
                    Map<String, Object> seg = new HashMap<>();
                    seg.put("start_time", s.getStartTime());
                    seg.put("end_time", s.getEndTime());
                    seg.put("text", s.getCleanText());
                    return seg;
                })
                .collect(Collectors.toList());
            doc.put("segments", segmentList);

            // 全文（用于快速搜索）
            String fullText = segments.stream()
                .map(SubtitleSegment::getCleanText)
                .collect(Collectors.joining(" "));
            doc.put("full_text", fullText);

            // 创建时间
            if (video.getCreatedAt() != null) {
                doc.put("created_at", video.getCreatedAt()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }

            esClient.index(i -> i
                .index(INDEX_NAME)
                .id(video.getVideoId())
                .document(doc)
            );

            log.info("✅ 视频已索引到ES: videoId={}, segments={}",
                video.getVideoId(), segments.size());

        } catch (IOException e) {
            log.error("❌ 索引视频失败: videoId={}", video.getVideoId(), e);
        }
    }

    /**
     * 搜索字幕（返回视频列表）
     */
    public Map<String, Object> search(SubtitleSearchRequest request) {
        if (!isAvailable()) {
            return Map.of("error", "Elasticsearch 未启用", "results", List.of());
        }

        try {
            // 构建查询
            Query query = buildSearchQuery(request);

            SearchResponse<Map> response = esClient.search(s -> s
                .index(INDEX_NAME)
                .query(query)
                .highlight(h -> h
                    .fields("full_text", f -> f
                        .preTags("<mark>")
                        .postTags("</mark>")
                        .fragmentSize(150)
                        .numberOfFragments(3))
                    .fields("full_text.english", f -> f
                        .preTags("<mark>")
                        .postTags("</mark>")
                        .fragmentSize(150)
                        .numberOfFragments(3))
                )
                .from(request.getPage() * request.getSize())
                .size(request.getSize()),
                Map.class
            );

            // 转换结果
            List<SubtitleSearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                SubtitleSearchResult result = convertHitToResult(hit);
                results.add(result);
            }

            // 构建响应
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("results", results);
            responseMap.put("total", total);
            responseMap.put("page", request.getPage());
            responseMap.put("size", request.getSize());
            responseMap.put("totalPages", (int) Math.ceil(total / (double) request.getSize()));

            return responseMap;

        } catch (IOException e) {
            log.error("❌ 搜索失败: keyword={}", request.getKeyword(), e);
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索并返回精确时间点
     */
    public List<SubtitleSearchResult> searchWithTimestamp(String keyword, int limit) {
        if (!isAvailable()) {
            return List.of();
        }

        try {
            // 使用 nested query 搜索 segments
            SearchResponse<Map> response = esClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                    .nested(n -> n
                        .path("segments")
                        .query(nq -> nq
                            .multiMatch(m -> m
                                .query(keyword)
                                .fields("segments.text", "segments.text.english")
                            )
                        )
                        .innerHits(ih -> ih
                            .highlight(h -> h
                                .fields("segments.text", f -> f
                                    .preTags("<mark>")
                                    .postTags("</mark>"))
                            )
                            .size(5)
                        )
                    )
                )
                .size(limit),
                Map.class
            );

            List<SubtitleSearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                SubtitleSearchResult result = convertHitToResultWithSegments(hit);
                results.add(result);
            }

            return results;

        } catch (IOException e) {
            log.error("❌ 时间点搜索失败: keyword={}", keyword, e);
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 删除视频索引
     */
    public void deleteVideo(String videoId) {
        if (!isAvailable()) {
            return;
        }

        try {
            esClient.delete(d -> d
                .index(INDEX_NAME)
                .id(videoId)
            );
            log.info("✅ 视频索引已删除: videoId={}", videoId);
        } catch (IOException e) {
            log.error("❌ 删除索引失败: videoId={}", videoId, e);
        }
    }

    /**
     * 构建搜索查询
     */
    private Query buildSearchQuery(SubtitleSearchRequest request) {
        List<Query> mustQueries = new ArrayList<>();

        // 关键词搜索（同时搜索中文和英文字段）
        mustQueries.add(Query.of(q -> q
            .multiMatch(m -> m
                .query(request.getKeyword())
                .fields("full_text^2", "full_text.english", "title^1.5", "title.english")
                .type(TextQueryType.BestFields)
                .fuzziness("AUTO")
            )
        ));

        // 语言筛选
        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            mustQueries.add(Query.of(q -> q
                .term(t -> t
                    .field("language")
                    .value(request.getLanguage())
                )
            ));
        }

        return Query.of(q -> q
            .bool(b -> b.must(mustQueries))
        );
    }

    /**
     * 转换搜索结果
     */
    @SuppressWarnings("unchecked")
    private SubtitleSearchResult convertHitToResult(Hit<Map> hit) {
        Map<String, Object> source = hit.source();
        SubtitleSearchResult result = new SubtitleSearchResult();

        if (source != null) {
            result.setVideoId((String) source.get("video_id"));
            result.setTitle((String) source.get("title"));
            result.setChannel((String) source.get("channel"));
            result.setLanguage((String) source.get("language"));
            result.setThumbnailUrl((String) source.get("thumbnail_url"));

            Object duration = source.get("duration");
            if (duration instanceof Number) {
                result.setDuration(((Number) duration).intValue());
            }
        }

        result.setScore(hit.score());

        // 提取高亮
        if (hit.highlight() != null && !hit.highlight().isEmpty()) {
            List<String> highlights = new ArrayList<>();
            hit.highlight().forEach((field, fragments) -> highlights.addAll(fragments));
            result.setHighlights(highlights);
        }

        return result;
    }

    /**
     * 转换搜索结果（包含匹配的时间点）
     */
    @SuppressWarnings("unchecked")
    private SubtitleSearchResult convertHitToResultWithSegments(Hit<Map> hit) {
        SubtitleSearchResult result = convertHitToResult(hit);

        // 提取 inner_hits 中的匹配片段
        if (hit.innerHits() != null && hit.innerHits().containsKey("segments")) {
            List<SegmentMatch> matches = new ArrayList<>();

            InnerHitsResult innerHitsResult = hit.innerHits().get("segments");
            if (innerHitsResult != null && innerHitsResult.hits() != null) {
                for (Hit<Object> innerHit : innerHitsResult.hits().hits()) {
                    if (innerHit.source() instanceof Map) {
                        Map<String, Object> segSource = (Map<String, Object>) innerHit.source();
                        SegmentMatch match = new SegmentMatch();

                        Object startTime = segSource.get("start_time");
                        Object endTime = segSource.get("end_time");

                        if (startTime instanceof Number) {
                            match.setStartTime(((Number) startTime).doubleValue());
                        }
                        if (endTime instanceof Number) {
                            match.setEndTime(((Number) endTime).doubleValue());
                        }
                        match.setText((String) segSource.get("text"));

                        // 高亮文本
                        if (innerHit.highlight() != null &&
                            innerHit.highlight().containsKey("segments.text")) {
                            List<String> highlightList = innerHit.highlight().get("segments.text");
                            if (highlightList != null && !highlightList.isEmpty()) {
                                match.setHighlightedText(highlightList.get(0));
                            }
                        }

                        matches.add(match);
                    }
                }
            }

            result.setMatchedSegments(matches);
        }

        return result;
    }

    /**
     * 初始化索引
     */
    private void initIndex() {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();

            if (!exists) {
                // 创建索引（基础配置，IK 分词器需要手动配置）
                esClient.indices().create(c -> c
                    .index(INDEX_NAME)
                    .settings(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0")
                    )
                    .mappings(m -> m
                        .properties("video_id", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t
                            .fields("keyword", f -> f.keyword(k -> k))
                            .fields("english", f -> f.text(tx -> tx.analyzer("standard")))
                        ))
                        .properties("channel", p -> p.keyword(k -> k))
                        .properties("language", p -> p.keyword(k -> k))
                        .properties("thumbnail_url", p -> p.keyword(k -> k))
                        .properties("duration", p -> p.integer(i -> i))
                        .properties("full_text", p -> p.text(t -> t
                            .fields("english", f -> f.text(tx -> tx.analyzer("standard")))
                        ))
                        .properties("segments", p -> p.nested(n -> n
                            .properties("start_time", sp -> sp.float_(fl -> fl))
                            .properties("end_time", sp -> sp.float_(fl -> fl))
                            .properties("text", sp -> sp.text(t -> t
                                .fields("english", f -> f.text(tx -> tx.analyzer("standard")))
                            ))
                        ))
                        .properties("created_at", p -> p.date(d -> d))
                    )
                );
                log.info("✅ ES索引创建成功: {}", INDEX_NAME);
            } else {
                log.info("✅ ES索引已存在: {}", INDEX_NAME);
            }
        } catch (IOException e) {
            log.error("❌ 初始化索引失败", e);
        }
    }
}
