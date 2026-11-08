package com.example.finance.followread;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.finance.search.service.SubtitleSearchService;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    @Autowired(required = false)
    private SubtitleSearchService subtitleSearchService;

    private static final String SUBTITLE_DIR = "uploads/subtitles/";
    private static final String AUDIO_DIR = "uploads/audio/";
    
    // Filler words to remove (English)
    private static final Set<String> FILLER_WORDS_EN = new HashSet<>(Arrays.asList(
        "uh", "um", "you know", "like", "so", "well", "actually", "basically", "literally"
    ));
    
    // Filler words to remove (Chinese)
    private static final Set<String> FILLER_WORDS_ZH = new HashSet<>(Arrays.asList(
        "嗯", "那个", "就是", "然后", "这个", "那个", "呃", "啊", "哦"
    ));
    
    // Filler words to remove (Japanese)
    private static final Set<String> FILLER_WORDS_JA = new HashSet<>(Arrays.asList(
        "えー", "あの", "まあ", "その", "なんか", "っていうか", "てか"
    ));
    
    /**
     * 判断是否是中日韩语言（CJK）
     */
    private boolean isCJKLanguage(String language) {
        if (language == null) return false;
        String lang = language.toLowerCase();
        return lang.startsWith("zh") || lang.startsWith("ja") || 
               lang.startsWith("ko") || lang.equals("jpn") || 
               lang.equals("chi") || lang.equals("kor") ||
               lang.equals("zh-cn") || lang.equals("zh-tw") ||
               lang.equals("zh-hans") || lang.equals("zh-hant");
    }
    
    /**
     * 获取语言代码的简化形式（用于文件名）
     */
    private String getLanguageCode(String language) {
        if (language == null) return "en";
        String lang = language.toLowerCase();
        // 处理 zh-Hans, zh-Hant 等格式
        if (lang.startsWith("zh")) {
            if (lang.contains("hans") || lang.contains("cn")) return "zh";
            if (lang.contains("hant") || lang.contains("tw")) return "zh";
            return "zh";
        }
        if (lang.startsWith("ja")) return "ja";
        if (lang.startsWith("ko")) return "ko";
        return lang.split("-")[0]; // 取主要部分，如 en-US -> en
    }

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
        parseSubtitlesAsync(videoId, null);
    }
    
    /**
     * 异步解析字幕（支持指定语言）
     */
    @Async
    @Transactional
    public void parseSubtitlesAsync(Long videoId, String language) {
        Optional<YoutubeVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            return;
        }

        YoutubeVideo video = videoOpt.get();
        
        try {
            // 清除旧的句子和字幕片段（重新解析时）
            List<FollowReadSentence> oldSentences = sentenceRepository.findByYoutubeVideoId(video.getId());
            if (!oldSentences.isEmpty()) {
                log.info("🗑️ 清除旧句子: videoId={}, count={}", video.getVideoId(), oldSentences.size());
                sentenceRepository.deleteAll(oldSentences);
            }
            segmentRepository.deleteByVideoId(video.getId());

            // 标记为解析中
            video.setStatus("parsing");
            video.setSentenceCount(0);
            videoRepository.save(video);

            // Step 1: 如果视频信息还未获取，先获取
            if ("Loading...".equals(video.getTitle()) || video.getTitle() == null) {
                updateProgress(video, "正在获取视频信息...");
                fetchVideoMetadata(video);
                updateProgress(video, "视频信息获取完成 ✓");
            }
            
            // Step 2: 获取字幕（如果指定了语言，使用指定语言；否则使用检测到的语言）
            String targetLanguage = language != null ? language : video.getSubtitleLanguage();
            if (targetLanguage != null) {
                video.setSubtitleLanguage(targetLanguage);
                videoRepository.save(video);
            }
            updateProgress(video, "正在下载字幕文件...");
            List<SubtitleSegment> segments = fetchSubtitles(video, targetLanguage);
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

            // 索引到 Elasticsearch
            if (subtitleSearchService != null) {
                try {
                    subtitleSearchService.indexVideo(video, segments);
                } catch (Exception e) {
                    log.warn("⚠️ ES索引失败，不影响主流程: {}", e.getMessage());
                }
            }

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
        JsonNode root = readSubtitleMetadata(video);

        video.setTitle(root.path("title").asText());
        video.setDescription(root.path("description").asText());
        video.setDuration(root.path("duration").asInt());
        video.setChannel(root.path("channel").asText());
        video.setThumbnailUrl(root.path("thumbnail").asText());

        // 检查字幕可用性（支持多语言）
        JsonNode subtitles = root.path("subtitles");
        JsonNode automaticCaptions = root.path("automatic_captions");

        // 检测可用的字幕语言（优先顺序：en, zh, ja, ko）
        String detectedLanguage = detectAvailableSubtitleLanguage(subtitles, automaticCaptions);
        boolean hasSubtitle = detectedLanguage != null;

        video.setHasSubtitle(hasSubtitle);
        video.setSubtitleLanguage(detectedLanguage != null ? detectedLanguage : "en");

        videoRepository.save(video);

        log.info("检测到字幕语言: videoId={}, language={}, hasSubtitle={}",
            video.getVideoId(), detectedLanguage, hasSubtitle);
    }

    JsonNode readSubtitleMetadata(YoutubeVideo video) throws Exception {
        
        // 构建命令，如果有 cookies 文件则使用
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        command.add("--ignore-config");
        command.add("--no-playlist");
        
        // 检查是否有 cookies 文件
        String cookieFile = getCookieFilePath(video.getVideoId());
        if (cookieFile != null) {
            command.add("--cookies");
            command.add(cookieFile);
            log.info("🍪 使用 cookies 文件: {}", cookieFile);
        } else {
            log.info("ℹ️ 未找到 cookies 文件，尝试无 cookies 方式");
        }
        
        command.add("--dump-json");
        command.add("--no-download");
        command.add(video.getSourceUrl());
        
        ProcessBuilder pb = new ProcessBuilder(command);
        
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
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
        return mapper.readTree(json.toString());
        
    }

    private String detectAvailableSubtitleLanguage(JsonNode subtitles, JsonNode automaticCaptions) {
        // Language preference takes priority over whether captions are manual or automatic.
        Set<String> languages = new LinkedHashSet<>(List.of("en", "zh", "ja", "ko"));
        subtitles.fieldNames().forEachRemaining(key -> languages.add(key.replaceFirst("-orig$", "")));
        automaticCaptions.fieldNames().forEachRemaining(key -> languages.add(key.replaceFirst("-orig$", "")));
        for (String language : languages) {
            if (SubtitleTrackSelector.select(subtitles, automaticCaptions, language) != null) {
                return language;
            }
        }
        return null;
    }

    /**
     * 获取字幕（支持指定语言）
     */
    private List<SubtitleSegment> fetchSubtitles(YoutubeVideo video) throws Exception {
        return fetchSubtitles(video, null);
    }
    
    /**
     * 获取目标语言字幕；下载失败明确报错，不切换语言
     */
    private List<SubtitleSegment> fetchSubtitles(YoutubeVideo video, String language) throws Exception {
        // 确定要使用的语言
        String targetLanguage = language != null ? language : video.getSubtitleLanguage();
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = "en"; // 默认英文
        }
        
        JsonNode metadata;
        try {
            metadata = readSubtitleMetadata(video);
        } catch (Exception e) {
            throw new java.io.IOException("字幕信息获取失败，无法判断目标语言是否可用: " + targetLanguage, e);
        }
        SubtitleTrackSelector.Track track = SubtitleTrackSelector.select(
            metadata.path("subtitles"), metadata.path("automatic_captions"), targetLanguage);
        if (track == null) {
            throw new IllegalStateException("没有可用的目标语言字幕: " + targetLanguage);
        }
        updateProgress(video, "正在下载" + getLanguageDisplayName(targetLanguage) + "字幕...");
        List<SubtitleSegment> segments = downloadSelectedSubtitles(video, metadata, track, targetLanguage);
        if (segments.isEmpty()) {
            throw new java.io.IOException("目标语言字幕下载结果为空: " + targetLanguage);
        }
        video.setSubtitleLanguage(targetLanguage);
        videoRepository.save(video);
        
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
    
    List<SubtitleSegment> downloadSelectedSubtitles(YoutubeVideo video, JsonNode metadata,
            SubtitleTrackSelector.Track track, String language) throws Exception {
        // Freeze the exact URL selected from metadata, avoiding a second extraction that
        // could select a translated or dubbed track under the same language key.
        Path directory = Files.createTempDirectory(Paths.get(SUBTITLE_DIR), "subtitle-");
        try {
            var info = (com.fasterxml.jackson.databind.node.ObjectNode) metadata.deepCopy();
            var subtitles = info.putObject("subtitles");
            subtitles.putArray(track.key()).add(track.format());
            info.putObject("automatic_captions");
            info.remove("requested_subtitles");
            Path infoFile = directory.resolve("selected.info.json");
            new ObjectMapper().writeValue(infoFile.toFile(), info);
            List<String> command = new ArrayList<>(List.of("yt-dlp", "--ignore-config",
                "--load-info-json", infoFile.toString(), "--write-subs", "--no-write-auto-subs",
                "--sub-langs", "^" + track.key() + "$", "--sub-format", "vtt", "--skip-download",
                "--retries", "2", "-o", directory.resolve("caption.%(ext)s").toString()));
            String cookieFile = getCookieFilePath(video.getVideoId());
            if (cookieFile != null) {
                command.add("--cookies");
                command.add(cookieFile);
            }
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            // Drain output but do not expose signed subtitle URLs or cookies in logs.
            try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
                while (reader.readLine() != null) { }
            }
            int exitCode = process.waitFor();
            Path file = directory.resolve("caption." + track.key() + ".vtt");
            if (exitCode != 0 || !Files.isRegularFile(file) || Files.size(file) == 0) {
                throw new java.io.IOException("目标语言字幕下载失败: " + language
                    + " (track=" + track.key() + ", exit=" + exitCode + ")");
            }
            return parseVttFile(file.toString(), video.getId(), language);
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
    
    private String getLanguageDisplayName(String language) {
        if (language == null) return "英文";
        String lang = language.toLowerCase();
        if (lang.startsWith("zh")) return "中文";
        if (lang.startsWith("ja")) return "日文";
        if (lang.startsWith("ko")) return "韩文";
        if (lang.startsWith("en")) return "英文";
        return language;
    }
    
    /**
     * 解析 VTT 字幕文件（支持多语言）
     */
    private List<SubtitleSegment> parseVttFile(String filePath, Long videoId) throws Exception {
        return parseVttFile(filePath, videoId, null);
    }
    
    /**
     * 解析 VTT 字幕文件（支持多语言）
     */
    private List<SubtitleSegment> parseVttFile(String filePath, Long videoId, String language) throws Exception {
        List<SubtitleSegment> segments = new ArrayList<>();
        
        Path path = Paths.get(filePath);
        if (!Files.isRegularFile(path)) {
            throw new java.io.IOException("Subtitle file not found: " + filePath);
        }
        
        List<String> lines = Files.readAllLines(path);

        // 检测是否为 YouTube 自动生成字幕（滚动格式：每个 cue 包含旧行 + 新行）
        boolean isRollingFormat = false;
        for (String l : lines) {
            if (l.contains("<c>") || l.matches(".*<\\d{2}:\\d{2}:\\d{2}\\.\\d{3}>.*")) {
                isRollingFormat = true;
                break;
            }
        }

        int order = 0;
        int i = 0;

        while (i < lines.size()) {
            String line = lines.get(i).trim();

            // 跳过 WEBVTT 头部和空行
            if (line.isEmpty() || line.startsWith("WEBVTT") || line.startsWith("NOTE") || line.startsWith("Kind:") || line.startsWith("Language:")) {
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

                // 读取 cue 中所有文本行
                i++;
                List<String> cueLines = new ArrayList<>();
                while (i < lines.size() && !lines.get(i).trim().isEmpty() && !lines.get(i).contains("-->")) {
                    cueLines.add(lines.get(i).trim());
                    i++;
                }

                // 跳过零时长的 cue（YouTube 用来清屏旧文本的）
                if (endTime - startTime < 0.05) {
                    continue;
                }

                String rawText;
                if (isRollingFormat) {
                    // YouTube 滚动字幕格式：只取包含 <c> 标签的行（新内容行）
                    // 另一行是上一条已显示过的纯文本（重复内容），跳过
                    StringBuilder newContent = new StringBuilder();
                    for (String cueLine : cueLines) {
                        if (cueLine.contains("<c>") || cueLine.matches(".*<\\d{2}:\\d{2}:\\d{2}\\.\\d{3}>.*")) {
                            newContent.append(cueLine).append(" ");
                        }
                    }
                    rawText = newContent.toString().trim();
                    // 如果没有带标签的行，说明不是滚动格式的 cue，取所有行
                    if (rawText.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (String cueLine : cueLines) {
                            sb.append(cueLine).append(" ");
                        }
                        rawText = sb.toString().trim();
                    }
                } else {
                    // 普通 VTT：取所有行
                    StringBuilder sb = new StringBuilder();
                    for (String cueLine : cueLines) {
                        sb.append(cueLine).append(" ");
                    }
                    rawText = sb.toString().trim();
                }

                if (!rawText.isEmpty()) {
                    SubtitleSegment segment = new SubtitleSegment();
                    segment.setVideoId(videoId);
                    segment.setStartTime(startTime);
                    segment.setEndTime(endTime);
                    segment.setRawText(rawText);
                    segment.setCleanText(cleanText(rawText, language)); // 传入语言参数
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
     * 清洗字幕文本（支持多语言）
     */
    private String cleanText(String text) {
        return cleanText(text, null);
    }
    
    /**
     * 清洗字幕文本（根据语言选择不同策略）
     */
    private String cleanText(String text, String language) {
        // 1. 解码HTML实体
        text = decodeHtmlEntities(text);
        
        // 2. 移除 HTML 标签
        text = text.replaceAll("<[^>]+>", "");
        
        // 3. 移除多余空格
        text = text.replaceAll("\\s+", " ");
        
        // 4. 根据语言选择不同的字符过滤策略
        if (isCJKLanguage(language)) {
            // 中文/日文/韩文：保留所有Unicode字符，只移除控制字符
            // 保留中文标点：，。！？；：""''（）【】《》、·等
            text = text.replaceAll("[\\p{Cntrl}&&[^\n\r\t]]", "");
        } else {
            // 英文等：使用原有逻辑，保留基本ASCII字符和标点
        text = text.replaceAll("[^a-zA-Z0-9\\s,.!?'-]", "");
        }
        
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
        List<SentenceUnit> sentenceUnits = splitIntoSentences(mergedSegments, video.getSubtitleLanguage());
        updateProgress(video, String.format("句子切分完成，共 %d 个候选句子", sentenceUnits.size()));
        
        updateProgress(video, "正在过滤和保存学习句子...");
        int order = 0;
        int processed = 0;
        int filtered = 0;
        for (SentenceUnit unit : sentenceUnits) {
            processed++;
            // 过滤太短或太长的句子（根据语言选择不同策略）
            double duration = unit.endTime - unit.startTime;
            boolean shouldFilter = false;
            
            if (isCJKLanguage(video.getSubtitleLanguage())) {
                // 中文/日文：基于字符数
                int charCount = unit.text.length();
                if (charCount < 3 || charCount > 200 || duration < 0.5 || duration > 40) {
                    shouldFilter = true;
                }
            } else {
                // 英文等：基于单词数
                int wordCount = unit.text.split("\\s+").length;
            if (wordCount < 3 || wordCount > 80 || duration < 0.5 || duration > 40) {
                    shouldFilter = true;
                }
            }
            
            if (shouldFilter) {
                filtered++;
                String preview = unit.text.length() > 50 ? unit.text.substring(0, 50) + "..." : unit.text;
                if (isCJKLanguage(video.getSubtitleLanguage())) {
                    log.info("过滤句子: {} 字符, {:.2f}秒 ({}~{}) - {}", 
                        unit.text.length(), duration, unit.startTime, unit.endTime, preview);
                } else {
                    int wordCount = unit.text.split("\\s+").length;
                log.info("过滤句子: {} 词, {:.2f}秒 ({}~{}) - {}", 
                        wordCount, duration, unit.startTime, unit.endTime, preview);
                }
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
            sentence.setDifficulty(calculateDifficulty(unit.text, video.getDifficultyLevel(), video.getSubtitleLanguage()));
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
    private List<SentenceUnit> splitIntoSentences(List<SubtitleSegment> segments, String language) {
        List<SentenceUnit> units = new ArrayList<>();
        
        // 简化策略：每个合并后的字幕段就是一个句子，使用其准确的时间戳
        // 不再进行二次切分，避免时间估算不准确
        for (SubtitleSegment segment : segments) {
            String text = segment.getCleanText().trim();
            
            if (text.isEmpty()) {
                continue;
            }
            
            // 移除 filler words（根据语言）
            text = removeFillerWords(text, language);
            
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
     * 移除 filler words（支持多语言）
     */
    private String removeFillerWords(String text, String language) {
        if (isCJKLanguage(language)) {
            // 中文/日文：直接移除填充词，不需要空格分割
            String result = text;
            Set<String> fillerWords = getFillerWords(language);
            for (String filler : fillerWords) {
                result = result.replace(filler, "");
            }
            return result.trim();
        } else {
            // 英文等：使用空格分割，保留原始大小写
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
            Set<String> fillerWords = getFillerWords(language);

        for (String word : words) {
            String cleanWord = word.toLowerCase().replaceAll("[^a-z]", "");
                if (!fillerWords.contains(cleanWord)) {
                result.append(word).append(" ");
            }
        }

        return result.toString().trim();
    }
    }
    
    /**
     * 获取指定语言的填充词列表
     */
    private Set<String> getFillerWords(String language) {
        if (language == null) return FILLER_WORDS_EN;
        String lang = language.toLowerCase();
        if (lang.startsWith("zh")) return FILLER_WORDS_ZH;
        if (lang.startsWith("ja")) return FILLER_WORDS_JA;
        return FILLER_WORDS_EN;
    }

    /**
     * 计算难度（支持多语言）
     */
    private String calculateDifficulty(String text, String userPreference, String language) {
        if (!"auto".equals(userPreference)) {
            return userPreference;
        }
        
        if (isCJKLanguage(language)) {
            // 中文/日文：基于字符数评估
            int charCount = text.length();
            // 统计中文字符数（CJK统一表意文字）
            long cjkCharCount = text.codePoints()
                .filter(cp -> Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                             Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.HIRAGANA ||
                             Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.KATAKANA ||
                             Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.HANGUL_SYLLABLES)
                .count();
            
            // 基于字符数和CJK字符比例评估难度
            if (charCount < 15 && cjkCharCount > charCount * 0.7) {
                return "easy";
            } else if (charCount > 50 || cjkCharCount < charCount * 0.3) {
                return "hard";
            } else {
                return "medium";
            }
        } else {
            // 英文等：使用原有逻辑（基于单词数）
        String[] words = text.split("\\s+");
        int wordCount = words.length;
        int longWordCount = 0;
        
        for (String word : words) {
            if (word.length() > 8) {
                longWordCount++;
            }
        }
        
            double longWordRatio = wordCount > 0 ? longWordCount / (double) wordCount : 0;
        
        // 调整难度阈值以适应更长的句子
        if (wordCount < 12 && longWordRatio < 0.2) {
            return "easy";
        } else if (wordCount > 25 || longWordRatio > 0.4) {
            return "hard";
        } else {
            return "medium";
            }
        }
    }

    /**
     * 获取用户的所有视频任务
     */
    public List<YoutubeVideo> getUserVideos(Long userId) {
        return videoRepository.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取所有视频（供所有用户学习）
     */
    public List<YoutubeVideo> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 分页获取所有视频
     */
    public org.springframework.data.domain.Page<YoutubeVideo> getAllVideosPaged(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return videoRepository.findAllByOrderByPinnedDescPinnedAtDescCreatedAtDesc(pageable);
    }

    /**
     * 切换视频置顶状态
     */
    @Transactional
    public YoutubeVideo togglePin(Long videoId) {
        Optional<YoutubeVideo> videoOpt = videoRepository.findById(videoId);
        if (!videoOpt.isPresent()) {
            throw new RuntimeException("Video not found");
        }
        YoutubeVideo video = videoOpt.get();
        boolean newPinned = !Boolean.TRUE.equals(video.getPinned());
        video.setPinned(newPinned);
        video.setPinnedAt(newPinned ? LocalDateTime.now() : null);
        return videoRepository.save(video);
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

        // 4. 删除 ES 索引
        if (subtitleSearchService != null) {
            try {
                subtitleSearchService.deleteVideo(video.getVideoId());
            } catch (Exception e) {
                log.warn("⚠️ 删除ES索引失败: {}", e.getMessage());
            }
        }

        // 5. 可选：删除字幕文件
        try {
            // 根据视频的语言选择字幕文件名
            String langCode = video.getSubtitleLanguage() != null ? 
                getLanguageCode(video.getSubtitleLanguage()) : "en";
            String subtitleFile = SUBTITLE_DIR + video.getVideoId() + "." + langCode + ".vtt";
            Path path = Paths.get(subtitleFile);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("已删除字幕文件: {}", subtitleFile);
            }
        } catch (Exception e) {
            log.warn("删除字幕文件失败: {}", e.getMessage());
        }
    }

    /**
     * 处理从浏览器提取的字幕（Chrome Extension 专用）
     * 这个方法不需要 yt-dlp，直接处理前端传来的字幕数据
     */
    @Transactional
    public YoutubeVideo processSubtitlesFromBrowser(
            String videoId, 
            String videoUrl, 
            Map<String, Object> metadata,
            List<Map<String, Object>> browserSubtitles,
            String cookies) throws Exception {
        
        log.info("处理浏览器字幕: videoId={}, 字幕数={}, cookies={}", 
            videoId, browserSubtitles.size(), cookies != null ? "已提供" : "未提供");
        
        // 如果提供了 cookies，保存到临时文件，以便后续需要时使用
        if (cookies != null && !cookies.isEmpty()) {
            saveCookiesToFile(videoId, cookies);
        }
        
        // 检查是否已经存在
        Optional<YoutubeVideo> existingOpt = videoRepository.findByVideoId(videoId);
        YoutubeVideo video;
        
        if (existingOpt.isPresent()) {
            video = existingOpt.get();
            log.info("视频已存在，更新数据: videoId={}", videoId);
        } else {
            // 创建新视频记录
            video = new YoutubeVideo();
            video.setVideoId(videoId);
            video.setSourceUrl(videoUrl != null ? videoUrl : "https://www.youtube.com/watch?v=" + videoId);
            video.setCreatedBy(1L); // 默认用户
            video.setDifficultyLevel("auto");
        }
        
        // 设置元数据
        if (metadata != null) {
            video.setTitle((String) metadata.getOrDefault("title", "Unknown Title"));
            video.setDescription((String) metadata.getOrDefault("description", ""));
            video.setDuration(((Number) metadata.getOrDefault("duration", 0)).intValue());
            video.setChannel((String) metadata.getOrDefault("channel", ""));
            video.setThumbnailUrl((String) metadata.getOrDefault("thumbnailUrl", ""));
        }
        
        video.setHasSubtitle(true);
        // 从 metadata 中获取语言，如果没有则默认为 en
        String detectedLanguage = "en";
        if (metadata != null && metadata.containsKey("language")) {
            detectedLanguage = (String) metadata.get("language");
        }
        video.setSubtitleLanguage(detectedLanguage);
        video.setStatus("parsing");
        video.setProgressMessage("正在处理浏览器字幕...");
        videoRepository.save(video);
        
        try {
            // 转换浏览器字幕为 SubtitleSegment
            List<SubtitleSegment> segments = new ArrayList<>();
            for (int i = 0; i < browserSubtitles.size(); i++) {
                Map<String, Object> sub = browserSubtitles.get(i);
                
                SubtitleSegment segment = new SubtitleSegment();
                segment.setVideoId(video.getId());
                segment.setStartTime(((Number) sub.get("startTime")).doubleValue());
                segment.setEndTime(((Number) sub.get("endTime")).doubleValue());
                segment.setRawText((String) sub.get("text"));
                segment.setCleanText(cleanText((String) sub.get("text"), video.getSubtitleLanguage()));
                segment.setSegmentOrder(i);
                
                segments.add(segment);
                segmentRepository.save(segment);
            }
            
            log.info("保存了 {} 个字幕片段", segments.size());
            
            // 生成学习句子
            generateLearningSentences(video, segments);

            // 标记完成
            video.setStatus("completed");
            video.setProgressMessage("字幕解析完成！");
            video.setCompletedAt(LocalDateTime.now());
            videoRepository.save(video);

            // 索引到 Elasticsearch
            if (subtitleSearchService != null) {
                try {
                    subtitleSearchService.indexVideo(video, segments);
                } catch (Exception e) {
                    log.warn("⚠️ ES索引失败，不影响主流程: {}", e.getMessage());
                }
            }

            log.info("✅ 浏览器字幕处理完成: videoId={}, sentences={}", 
                video.getVideoId(), video.getSentenceCount());
            
            return video;
            
        } catch (Exception e) {
            video.setStatus("failed");
            video.setErrorMessage(e.getMessage());
            video.setProgressMessage("处理失败: " + e.getMessage());
            videoRepository.save(video);
            log.error("❌ 浏览器字幕处理失败: videoId={}", videoId, e);
            throw e;
        }
    }

    /**
     * 保存 cookies 到临时文件
     */
    private void saveCookiesToFile(String videoId, String cookies) {
        try {
            if (cookies == null || cookies.trim().isEmpty()) {
                log.warn("⚠️ Cookies 为空，无法保存: videoId={}", videoId);
                return;
            }
            
            String cookieFilePath = "/tmp/youtube_cookies_" + videoId + ".txt";
            Path cookiePath = Paths.get(cookieFilePath);
            
            // 确保目录存在
            Files.createDirectories(cookiePath.getParent());
            
            // 写入文件
            Files.write(cookiePath, cookies.getBytes(StandardCharsets.UTF_8));
            
            // 验证文件是否写入成功
            if (Files.exists(cookiePath)) {
                long fileSize = Files.size(cookiePath);
                log.info("✅ Cookies 已保存到: {} (大小: {} bytes)", cookieFilePath, fileSize);
            } else {
                log.error("❌ Cookies 文件保存后验证失败: {}", cookieFilePath);
            }
        } catch (Exception e) {
            log.error("❌ 保存 cookies 失败: videoId={}, error={}", videoId, e.getMessage(), e);
        }
    }
    
    /**
     * 为视频保存 cookies（public 方法，供 Controller 调用）
     */
    public void saveCookiesForVideo(String videoId, String cookies) {
        saveCookiesToFile(videoId, cookies);
    }
    
    /**
     * 获取 cookies 文件路径（如果存在）
     */
    private String getCookieFilePath(String videoId) {
        String cookieFilePath = "/tmp/youtube_cookies_" + videoId + ".txt";
        if (Files.exists(Paths.get(cookieFilePath))) {
            log.info("✅ 找到 cookies 文件: {}", cookieFilePath);
            return cookieFilePath;
        }
        return null;
    }

    // 内部类：句子单元
    private static class SentenceUnit {
        String text;
        double startTime;
        double endTime;
    }
}

