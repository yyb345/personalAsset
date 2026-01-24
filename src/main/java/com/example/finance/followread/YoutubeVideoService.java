package com.example.finance.followread;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YoutubeVideoService {

    private static final Logger log = LoggerFactory.getLogger(YoutubeVideoService.class);

    @Autowired
    private YoutubeVideoRepository videoRepository;

    @Autowired
    private SubtitleSegmentRepository segmentRepository;

    @Autowired
    private FollowReadSentenceRepository sentenceRepository;

    private static final String SUBTITLE_DIR = "uploads/subtitles/";
    private static final String AUDIO_DIR = "uploads/audio/";
    
    // Filler words to remove
    private static final Set<String> FILLER_WORDS = new HashSet<>(Arrays.asList(
        "uh", "um", "you know", "like", "so", "well", "actually", "basically", "literally"
    ));

    public YoutubeVideoService() {
        // 确保目录存在
        new File(SUBTITLE_DIR).mkdirs();
        new File(AUDIO_DIR).mkdirs();
    }

    /**
     * 从 YouTube URL 提取视频 ID
     */
    public String extractVideoId(String url) {
        // 支持多种 URL 格式
        // https://www.youtube.com/watch?v=VIDEO_ID
        // https://youtu.be/VIDEO_ID
        // https://www.youtube.com/embed/VIDEO_ID
        
        Pattern pattern = Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})");
        Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid YouTube URL");
    }

    /**
     * 检查 yt-dlp 是否安装
     */
    public boolean isYtDlpInstalled() {
        try {
            Process process = new ProcessBuilder("yt-dlp", "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 添加视频到库（仅获取基本信息，不解析字幕）
     */
    public YoutubeVideo addVideoToLibrary(String url, Long userId, String difficulty) {
        String videoId = extractVideoId(url);
        
        // 检查是否已经存在
        Optional<YoutubeVideo> existing = videoRepository.findByVideoId(videoId);
        if (existing.isPresent()) {
            return existing.get();
        }

        YoutubeVideo video = new YoutubeVideo();
        video.setVideoId(videoId);
        video.setSourceUrl(url);
        video.setStatus("added"); // 新状态：已添加但未解析字幕
        video.setCreatedBy(userId);
        video.setDifficultyLevel(difficulty != null ? difficulty : "auto");
        video.setSentenceCount(0);
        
        // Set placeholder values for required fields (will be updated after metadata fetch)
        video.setTitle("Loading...");
        video.setDuration(0);
        video.setProgressMessage("已添加到视频库");
        
        return videoRepository.save(video);
    }

    /**
     * 异步获取视频基本信息（不解析字幕）
     */
    @Async
    @Transactional
    public void fetchVideoInfoAsync(Long videoId) {
        Optional<YoutubeVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            return;
        }

        YoutubeVideo video = videoOpt.get();
        
        try {
            // 只获取视频元数据
            updateProgress(video, "正在获取视频信息...");
            fetchVideoMetadata(video);
            
            // 标记为已添加（信息已完整）
            video.setStatus("added");
            video.setProgressMessage("视频信息已获取，可以解析字幕");
            videoRepository.save(video);
            
            log.info("✅ 视频信息获取完成: videoId={}, title={}", video.getVideoId(), video.getTitle());
            
        } catch (Exception e) {
            video.setStatus("failed");
            video.setErrorMessage(e.getMessage());
            video.setProgressMessage("获取视频信息失败: " + e.getMessage());
            videoRepository.save(video);
            log.error("❌ 获取视频信息失败: videoId={}", video.getVideoId(), e);
        }
    }

    /**
     * 异步解析字幕（仅解析字幕和生成学习句子）
     * 前提：视频信息已经获取完成
     */
    @Async
    @Transactional
    public void parseSubtitlesAsync(Long videoId) {
        Optional<YoutubeVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            return;
        }

        YoutubeVideo video = videoOpt.get();
        
        try {
            // 标记为解析中
            video.setStatus("parsing");
            videoRepository.save(video);
            
            // Step 1: 如果视频信息还未获取，先获取
            if ("Loading...".equals(video.getTitle()) || video.getTitle() == null) {
                updateProgress(video, "正在获取视频信息...");
                fetchVideoMetadata(video);
                updateProgress(video, "视频信息获取完成 ✓");
            }
            
            // Step 2: 获取字幕
            updateProgress(video, "正在下载字幕文件...");
            List<SubtitleSegment> segments = fetchSubtitles(video);
            updateProgress(video, String.format("字幕下载完成 ✓ (共 %d 个片段)", segments.size()));
            
            // Step 3: 生成学习句子
            updateProgress(video, "正在智能切分句子...");
            generateLearningSentences(video, segments);
            updateProgress(video, String.format("句子生成完成 ✓ (共 %d 个学习句子)", video.getSentenceCount()));
            
            // 标记完成
            video.setStatus("completed");
            video.setProgressMessage("字幕解析完成！");
            video.setCompletedAt(LocalDateTime.now());
            videoRepository.save(video);
            
            log.info("✅ 字幕解析完成: videoId={}, sentences={}", video.getVideoId(), video.getSentenceCount());
            
        } catch (Exception e) {
            video.setStatus("failed");
            video.setErrorMessage(e.getMessage());
            video.setProgressMessage("字幕解析失败: " + e.getMessage());
            videoRepository.save(video);
            log.error("❌ 字幕解析失败: videoId={}", video.getVideoId(), e);
        }
    }
    
    /**
     * 更新解析进度
     */
    private void updateProgress(YoutubeVideo video, String message) {
        video.setProgressMessage(message);
        videoRepository.save(video);
        log.info("[YouTube解析] 视频ID: {} - {}", video.getVideoId(), message);
    }

    /**
     * 获取视频元数据
     */
    private void fetchVideoMetadata(YoutubeVideo video) throws Exception {
        // 使用 yt-dlp 获取视频信息
        updateProgress(video, "正在连接YouTube服务器...");
        ProcessBuilder pb = new ProcessBuilder(
            "yt-dlp",
            "--dump-json",
            "--no-download",
            video.getSourceUrl()
        );
        
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder json = new StringBuilder();
        String line;
        
        updateProgress(video, "正在读取视频元数据...");
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to fetch video metadata");
        }

        // 解析 JSON
        updateProgress(video, "正在解析视频信息...");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json.toString());
        
        video.setTitle(root.path("title").asText());
        video.setDescription(root.path("description").asText());
        video.setDuration(root.path("duration").asInt());
        video.setChannel(root.path("channel").asText());
        video.setThumbnailUrl(root.path("thumbnail").asText());
        
        // 检查字幕可用性
        JsonNode subtitles = root.path("subtitles");
        JsonNode automaticCaptions = root.path("automatic_captions");
        
        boolean hasSubtitle = subtitles.has("en") || automaticCaptions.has("en");
        video.setHasSubtitle(hasSubtitle);
        video.setSubtitleLanguage("en");
        
        videoRepository.save(video);
    }

    /**
     * 获取字幕
     */
    private List<SubtitleSegment> fetchSubtitles(YoutubeVideo video) throws Exception {
        String subtitleFile = SUBTITLE_DIR + video.getVideoId() + ".vtt";
        
        // 使用 yt-dlp 下载字幕
        updateProgress(video, "正在请求字幕下载...");
        ProcessBuilder pb = new ProcessBuilder(
            "yt-dlp",
            "--write-sub",
            "--write-auto-sub",
            "--sub-lang", "en",
            "--sub-format", "vtt",
            "--skip-download",
            "-o", SUBTITLE_DIR + video.getVideoId(),
            video.getSourceUrl()
        );
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("Failed to download subtitles");
        }

        // 解析 VTT 字幕文件
        updateProgress(video, "正在解析字幕文件...");
        List<SubtitleSegment> segments = parseVttFile(subtitleFile, video.getId());
        
        // 保存字幕片段
        updateProgress(video, String.format("正在保存字幕片段 (0/%d)...", segments.size()));
        int count = 0;
        for (SubtitleSegment segment : segments) {
            segmentRepository.save(segment);
            count++;
            // 每100个片段更新一次进度
            if (count % 100 == 0 || count == segments.size()) {
                updateProgress(video, String.format("正在保存字幕片段 (%d/%d)...", count, segments.size()));
            }
        }
        
        return segments;
    }

    /**
     * 解析 VTT 字幕文件
     */
    private List<SubtitleSegment> parseVttFile(String filePath, Long videoId) throws Exception {
        List<SubtitleSegment> segments = new ArrayList<>();
        
        // 尝试多个可能的文件名
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            // 尝试 .en.vtt 后缀
            path = Paths.get(filePath.replace(".vtt", ".en.vtt"));
        }
        
        if (!Files.exists(path)) {
            throw new RuntimeException("Subtitle file not found: " + filePath);
        }
        
        List<String> lines = Files.readAllLines(path);
        
        int order = 0;
        int i = 0;
        
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            
            // 跳过 WEBVTT 头部和空行
            if (line.isEmpty() || line.startsWith("WEBVTT") || line.startsWith("NOTE")) {
                i++;
                continue;
            }
            
            // 时间戳行格式: 00:00:00.000 --> 00:00:03.000 [可能有额外属性]
            if (line.contains("-->")) {
                String[] times = line.split("-->");
                // 清理时间戳，移除额外的属性（如 align:start position:0%）
                String startTimeStr = times[0].trim();
                String endTimeStr = times[1].trim().split("\\s+")[0]; // 只取第一个空格前的部分
                
                double startTime = parseVttTime(startTimeStr);
                double endTime = parseVttTime(endTimeStr);
                
                // 读取文本内容
                i++;
                StringBuilder text = new StringBuilder();
                while (i < lines.size() && !lines.get(i).trim().isEmpty() && !lines.get(i).contains("-->")) {
                    text.append(lines.get(i).trim()).append(" ");
                    i++;
                }
                
                String rawText = text.toString().trim();
                if (!rawText.isEmpty()) {
                    SubtitleSegment segment = new SubtitleSegment();
                    segment.setVideoId(videoId);
                    segment.setStartTime(startTime);
                    segment.setEndTime(endTime);
                    segment.setRawText(rawText);
                    segment.setCleanText(cleanText(rawText));
                    segment.setSegmentOrder(order++);
                    segments.add(segment);
                }
            } else {
                i++;
            }
        }
        
        return segments;
    }

    /**
     * 解析 VTT 时间戳（格式: 00:00:00.000 或 00:00.000）
     */
    private double parseVttTime(String timeStr) {
        String[] parts = timeStr.split(":");
        double seconds = 0;
        
        if (parts.length == 3) {
            // HH:MM:SS.mmm
            seconds = Integer.parseInt(parts[0]) * 3600 +
                     Integer.parseInt(parts[1]) * 60 +
                     Double.parseDouble(parts[2]);
        } else if (parts.length == 2) {
            // MM:SS.mmm
            seconds = Integer.parseInt(parts[0]) * 60 +
                     Double.parseDouble(parts[1]);
        }
        
        return seconds;
    }

    /**
     * 清洗字幕文本
     */
    private String cleanText(String text) {
        // 1. 解码HTML实体
        text = decodeHtmlEntities(text);
        
        // 2. 移除 HTML 标签
        text = text.replaceAll("<[^>]+>", "");
        
        // 3. 移除多余空格
        text = text.replaceAll("\\s+", " ");
        
        // 4. 移除特殊字符（保留基本标点）
        text = text.replaceAll("[^a-zA-Z0-9\\s,.!?'-]", "");
        
        return text.trim();
    }
    
    /**
     * 解码常见的HTML实体
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) return "";
        
        // 常见HTML实体替换
        text = text.replace("&nbsp;", " ");
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&#39;", "'");
        text = text.replace("&apos;", "'");
        text = text.replace("&ndash;", "-");
        text = text.replace("&mdash;", "-");
        text = text.replace("&hellip;", "...");
        
        // 处理数字实体 (如 &#160; 是 nbsp)
        text = text.replaceAll("&#160;", " ");
        text = text.replaceAll("&#\\d+;", "");
        
        return text;
    }

    /**
     * 生成学习句子（智能切分）
     */
    private void generateLearningSentences(YoutubeVideo video, List<SubtitleSegment> segments) {
        List<FollowReadSentence> sentences = new ArrayList<>();
        
        // 合并相邻字幕段（间隔 < 2.5s，在准确性和句子长度间平衡）
        updateProgress(video, "正在合并字幕片段...");
        List<SubtitleSegment> mergedSegments = mergeNearbySegments(segments, 2.5);
        updateProgress(video, String.format("字幕合并完成，共 %d 个片段（原始 %d 个）", mergedSegments.size(), segments.size()));
        
        log.info("字幕合并: {}个原始片段 → {}个合并片段", segments.size(), mergedSegments.size());
        
        // 按语义切分句子
        updateProgress(video, "正在按语义切分句子...");
        List<SentenceUnit> sentenceUnits = splitIntoSentences(mergedSegments);
        updateProgress(video, String.format("句子切分完成，共 %d 个候选句子", sentenceUnits.size()));
        
        updateProgress(video, "正在过滤和保存学习句子...");
        int order = 0;
        int processed = 0;
        int filtered = 0;
        for (SentenceUnit unit : sentenceUnits) {
            processed++;
            // 过滤太短或太长的句子（更宽松的范围）
            int wordCount = unit.text.split("\\s+").length;
            double duration = unit.endTime - unit.startTime;
            
            // 放宽过滤条件：单词数 3-80个，时长 0.5-40秒
            if (wordCount < 3 || wordCount > 80 || duration < 0.5 || duration > 40) {
                filtered++;
                log.info("过滤句子: {} 词, {:.2f}秒 ({}~{}) - {}", 
                    wordCount, duration, unit.startTime, unit.endTime, 
                    unit.text.substring(0, Math.min(50, unit.text.length())));
                continue; // 跳过不适合跟读的句子
            }
            
            // 每10个句子更新一次进度
            if (processed % 10 == 0 || processed == sentenceUnits.size()) {
                updateProgress(video, String.format("正在处理句子 (%d/%d，已过滤 %d)...", processed, sentenceUnits.size(), filtered));
            }
            
            FollowReadSentence sentence = new FollowReadSentence();
            sentence.setText(unit.text);
            sentence.setPhonetic(""); // TODO: 生成音标
            sentence.setAudioUrl(null); // YouTube句子使用视频片段，不需要单独的音频文件
            sentence.setDifficulty(calculateDifficulty(unit.text, video.getDifficultyLevel()));
            sentence.setCategory("YouTube");
            sentence.setYoutubeVideoId(video.getId());
            sentence.setStartTime(unit.startTime);
            sentence.setEndTime(unit.endTime);
            sentence.setSentenceOrder(order++);
            sentence.setVideoUrl(video.getSourceUrl());
            
            sentenceRepository.save(sentence);
            sentences.add(sentence);
        }
        
        video.setSentenceCount(sentences.size());
        videoRepository.save(video);
        
        log.info("句子生成统计: 候选{}个, 已保存{}个, 已过滤{}个", sentenceUnits.size(), sentences.size(), filtered);
    }

    /**
     * 合并相邻的字幕段（改进版：限制合并长度）
     */
    private List<SubtitleSegment> mergeNearbySegments(List<SubtitleSegment> segments, double maxGap) {
        if (segments.isEmpty()) {
            return segments;
        }
        
        List<SubtitleSegment> merged = new ArrayList<>();
        SubtitleSegment current = segments.get(0);
        
        for (int i = 1; i < segments.size(); i++) {
            SubtitleSegment next = segments.get(i);
            double gap = next.getStartTime() - current.getEndTime();
            
            // 计算合并后的长度
            int currentWordCount = current.getCleanText().split("\\s+").length;
            int nextWordCount = next.getCleanText().split("\\s+").length;
            double mergedDuration = next.getEndTime() - current.getStartTime();
            
            // 合并条件：
            // 1. 时间间隔 <= maxGap
            // 2. 合并后单词数 <= 60
            // 3. 合并后时长 <= 30秒
            if (gap <= maxGap && 
                (currentWordCount + nextWordCount) <= 60 && 
                mergedDuration <= 30) {
                // 合并
                current.setEndTime(next.getEndTime());
                current.setCleanText(current.getCleanText() + " " + next.getCleanText());
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        
        return merged;
    }

    /**
     * 按语义切分成句子（改进版：直接使用字幕段的准确时间）
     */
    private List<SentenceUnit> splitIntoSentences(List<SubtitleSegment> segments) {
        List<SentenceUnit> units = new ArrayList<>();
        
        // 简化策略：每个合并后的字幕段就是一个句子，使用其准确的时间戳
        // 不再进行二次切分，避免时间估算不准确
        for (SubtitleSegment segment : segments) {
            String text = segment.getCleanText().trim();
            
            if (text.isEmpty()) {
                continue;
            }
            
            // 移除 filler words
            text = removeFillerWords(text);
            
            if (text.isEmpty()) {
                continue;
            }
            
            SentenceUnit unit = new SentenceUnit();
            unit.text = text;
            unit.startTime = segment.getStartTime();  // 使用字幕段的准确开始时间
            unit.endTime = segment.getEndTime();      // 使用字幕段的准确结束时间
            
            units.add(unit);
        }
        
        return units;
    }

    /**
     * 移除 filler words
     */
    private String removeFillerWords(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-z]", "");
            if (!FILLER_WORDS.contains(cleanWord)) {
                result.append(word).append(" ");
            }
        }
        
        return result.toString().trim();
    }

    /**
     * 计算难度
     */
    private String calculateDifficulty(String text, String userPreference) {
        if (!"auto".equals(userPreference)) {
            return userPreference;
        }
        
        // 简单的难度评估算法（适配更长的句子）
        String[] words = text.split("\\s+");
        int wordCount = words.length;
        int longWordCount = 0;
        
        for (String word : words) {
            if (word.length() > 8) {
                longWordCount++;
            }
        }
        
        double longWordRatio = longWordCount / (double) wordCount;
        
        // 调整难度阈值以适应更长的句子
        if (wordCount < 12 && longWordRatio < 0.2) {
            return "easy";
        } else if (wordCount > 25 || longWordRatio > 0.4) {
            return "hard";
        } else {
            return "medium";
        }
    }

    /**
     * 获取用户的所有视频任务
     */
    public List<YoutubeVideo> getUserVideos(Long userId) {
        return videoRepository.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取视频详情（包括生成的句子）
     */
    public Map<String, Object> getVideoDetails(Long videoId) {
        Optional<YoutubeVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            throw new RuntimeException("Video not found");
        }

        YoutubeVideo video = videoOpt.get();
        List<FollowReadSentence> sentences = sentenceRepository.findByYoutubeVideoId(videoId);
        
        Map<String, Object> details = new HashMap<>();
        details.put("video", video);
        details.put("sentences", sentences);
        details.put("totalSentences", sentences.size());
        
        return details;
    }

    /**
     * 删除视频及其所有相关数据（级联删除）
     */
    @Transactional
    public void deleteVideo(Long videoId) {
        Optional<YoutubeVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            throw new RuntimeException("Video not found");
        }

        YoutubeVideo video = videoOpt.get();
        
        log.info("删除视频及相关数据: videoId={}, title={}", video.getVideoId(), video.getTitle());
        
        // 1. 删除所有生成的句子
        List<FollowReadSentence> sentences = sentenceRepository.findByYoutubeVideoId(videoId);
        int sentenceCount = sentences.size();
        sentenceRepository.deleteAll(sentences);
        log.info("已删除 {} 个学习句子", sentenceCount);
        
        // 2. 删除所有字幕片段
        segmentRepository.deleteByVideoId(videoId);
        log.info("已删除字幕片段");
        
        // 3. 删除视频记录
        videoRepository.delete(video);
        log.info("已删除视频记录: {}", video.getVideoId());
        
        // 4. 可选：删除字幕文件
        try {
            String subtitleFile = SUBTITLE_DIR + video.getVideoId() + ".en.vtt";
            Path path = Paths.get(subtitleFile);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("已删除字幕文件: {}", subtitleFile);
            }
        } catch (Exception e) {
            log.warn("删除字幕文件失败: {}", e.getMessage());
        }
    }

    // 内部类：句子单元
    private static class SentenceUnit {
        String text;
        double startTime;
        double endTime;
    }
}

