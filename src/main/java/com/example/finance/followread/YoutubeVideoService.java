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
        // 使用 yt-dlp 获取视频信息
        updateProgress(video, "正在连接YouTube服务器...");
        
        // 构建命令，如果有 cookies 文件则使用
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        
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
        
        // 检查字幕可用性（支持多语言）
        JsonNode subtitles = root.path("subtitles");
        JsonNode automaticCaptions = root.path("automatic_captions");
        
        // 检测可用的字幕语言（优先顺序：en, zh, ja, ko, 其他）
        String detectedLanguage = detectAvailableSubtitleLanguage(subtitles, automaticCaptions);
        boolean hasSubtitle = detectedLanguage != null;
        
        video.setHasSubtitle(hasSubtitle);
        video.setSubtitleLanguage(detectedLanguage != null ? detectedLanguage : "en");
        
        videoRepository.save(video);
        
        log.info("检测到字幕语言: videoId={}, language={}, hasSubtitle={}", 
            video.getVideoId(), detectedLanguage, hasSubtitle);
    }

    /**
     * 检测可用的字幕语言
     */
    private String detectAvailableSubtitleLanguage(JsonNode subtitles, JsonNode automaticCaptions) {
        // 优先顺序：en, zh, ja, ko, 其他
        String[] preferredLanguages = {"en", "zh", "zh-Hans", "zh-Hant", "ja", "ko"};
        
        // 先检查官方字幕
        for (String lang : preferredLanguages) {
            if (subtitles.has(lang)) {
                return lang;
            }
        }
        
        // 再检查自动生成字幕
        for (String lang : preferredLanguages) {
            if (automaticCaptions.has(lang)) {
                return lang;
            }
        }
        
        // 如果都没有，返回第一个可用的
        if (subtitles.isObject() && subtitles.size() > 0) {
            return subtitles.fieldNames().next();
        }
        if (automaticCaptions.isObject() && automaticCaptions.size() > 0) {
            return automaticCaptions.fieldNames().next();
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
     * 获取字幕（支持指定语言，如果失败会自动尝试其他语言）
     */
    private List<SubtitleSegment> fetchSubtitles(YoutubeVideo video, String language) throws Exception {
        // 确定要使用的语言
        String targetLanguage = language != null ? language : video.getSubtitleLanguage();
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = "en"; // 默认英文
        }
        
        // 尝试下载字幕：先尝试指定语言，如果失败则尝试所有可用语言
        List<String> languagesToTry = new ArrayList<>();
        languagesToTry.add(targetLanguage);
        
        // 添加常见语言作为备选
        String[] fallbackLanguages = {"zh", "zh-Hans", "zh-Hant", "zh-CN", "zh-TW", "en", "ja", "ko"};
        for (String lang : fallbackLanguages) {
            if (!languagesToTry.contains(lang)) {
                languagesToTry.add(lang);
            }
        }
        
        Exception lastException = null;
        String downloadedLanguage = null;
        String downloadedFilePath = null;
        
        // 策略1: 先尝试指定语言
        for (String langToTry : languagesToTry) {
            try {
                log.info("尝试下载字幕: videoId={}, language={}", video.getVideoId(), langToTry);
                updateProgress(video, String.format("正在下载%s字幕...", getLanguageDisplayName(langToTry)));
                
                if (downloadSubtitleWithLanguage(video, langToTry)) {
                    // 尝试多种可能的文件名格式
                    File subtitleFile = findSubtitleFile(video.getVideoId(), langToTry);
                    if (subtitleFile != null && subtitleFile.exists()) {
                        downloadedLanguage = langToTry;
                        downloadedFilePath = subtitleFile.getAbsolutePath();
                        log.info("✅ 成功下载字幕: videoId={}, language={}, file={}", 
                            video.getVideoId(), langToTry, downloadedFilePath);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("下载字幕失败: videoId={}, language={}, error={}", 
                    video.getVideoId(), langToTry, e.getMessage());
                lastException = e;
            }
        }
        
        // 策略2: 如果所有指定语言都失败，尝试下载所有可用字幕（不指定语言）
        if (downloadedFilePath == null) {
            try {
                log.info("尝试下载所有可用字幕: videoId={}", video.getVideoId());
                updateProgress(video, "正在下载所有可用字幕...");
                
                if (downloadAllAvailableSubtitles(video)) {
                    // 查找任何下载成功的字幕文件
                    File subtitleFile = findAnySubtitleFile(video.getVideoId());
                    if (subtitleFile != null && subtitleFile.exists()) {
                        downloadedFilePath = subtitleFile.getAbsolutePath();
                        // 从文件名推断语言
                        String fileName = subtitleFile.getName();
                        if (fileName.contains(".zh.") || fileName.contains(".zh-Hans.") || fileName.contains(".zh-Hant.")) {
                            downloadedLanguage = "zh";
                        } else if (fileName.contains(".en.")) {
                            downloadedLanguage = "en";
                        } else if (fileName.contains(".ja.")) {
                            downloadedLanguage = "ja";
                        } else if (fileName.contains(".ko.")) {
                            downloadedLanguage = "ko";
                        } else {
                            downloadedLanguage = targetLanguage; // 使用原始目标语言
                        }
                        log.info("✅ 成功下载字幕（所有语言）: videoId={}, file={}", 
                            video.getVideoId(), downloadedFilePath);
                    }
                }
            } catch (Exception e) {
                log.warn("下载所有字幕失败: videoId={}, error={}", video.getVideoId(), e.getMessage());
                lastException = e;
            }
        }
        
        // 如果仍然失败，抛出异常
        if (downloadedFilePath == null || !new File(downloadedFilePath).exists()) {
            log.error("❌ 所有字幕下载尝试都失败了: videoId={}", video.getVideoId());
            throw new RuntimeException("Failed to download subtitles after trying multiple languages. " +
                (lastException != null ? "Last error: " + lastException.getMessage() : ""));
        }
        
        // 更新视频的语言信息
        if (downloadedLanguage != null) {
            video.setSubtitleLanguage(downloadedLanguage);
            videoRepository.save(video);
        }
        
        // 解析 VTT 字幕文件
        updateProgress(video, "正在解析字幕文件...");
        List<SubtitleSegment> segments = parseVttFile(downloadedFilePath, video.getId(), downloadedLanguage);
        
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
     * 使用指定语言下载字幕
     */
    private boolean downloadSubtitleWithLanguage(YoutubeVideo video, String language) throws Exception {
        // 构建命令，如果有 cookies 文件则使用
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        
        // 检查是否有 cookies 文件
        String cookieFile = getCookieFilePath(video.getVideoId());
        if (cookieFile != null) {
            command.add("--cookies");
            command.add(cookieFile);
        }
        
        command.add("--write-sub");
        command.add("--write-auto-sub");
        command.add("--sub-lang");
        command.add(language);
        command.add("--sub-format");
        command.add("vtt");
        command.add("--skip-download");
        command.add("-o");
        command.add(SUBTITLE_DIR + "%(id)s.%(ext)s");
        command.add(video.getSourceUrl());

        log.info("🎬 yt-dlp command(videoId={}, language={}): {}", 
            video.getVideoId(), language, String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        if (output.length() > 0) {
            log.info("yt-dlp 输出: {}", output.toString());
        }
        
        // 检查输出中是否有 "There are no subtitles" 的提示
        if (output.toString().contains("There are no subtitles")) {
            log.warn("该语言没有字幕: videoId={}, language={}", video.getVideoId(), language);
            return false;
        }
        
        return exitCode == 0;
    }
    
    /**
     * 下载所有可用字幕（不指定语言）
     */
    private boolean downloadAllAvailableSubtitles(YoutubeVideo video) throws Exception {
        // 构建命令，如果有 cookies 文件则使用
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        
        // 检查是否有 cookies 文件
        String cookieFile = getCookieFilePath(video.getVideoId());
        if (cookieFile != null) {
            command.add("--cookies");
            command.add(cookieFile);
        }
        
        command.add("--write-sub");
        command.add("--write-auto-sub");
        // 不指定 --sub-lang，让 yt-dlp 下载所有可用字幕
        command.add("--sub-format");
        command.add("vtt");
        command.add("--skip-download");
        command.add("-o");
        command.add(SUBTITLE_DIR + "%(id)s.%(ext)s");
        command.add(video.getSourceUrl());

        log.info("🎬 yt-dlp command(videoId={}, all languages): {}", 
            video.getVideoId(), String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        if (output.length() > 0) {
            log.info("yt-dlp 输出: {}", output.toString());
        }
        
        return exitCode == 0;
    }
    
    /**
     * 查找字幕文件（支持多种文件名格式）
     */
    private File findSubtitleFile(String videoId, String language) {
        File subtitleDir = new File(SUBTITLE_DIR);
        if (!subtitleDir.exists()) {
            return null;
        }
        
        String langCode = getLanguageCode(language);
        
        // 尝试多种可能的文件名格式
        String[] possibleNames = {
            videoId + "." + langCode + ".vtt",
            videoId + "." + language + ".vtt",
            videoId + ".vtt"
        };
        
        for (String name : possibleNames) {
            File file = new File(subtitleDir, name);
            if (file.exists()) {
                return file;
            }
        }
        
        // 如果都不存在，列出所有匹配的文件
        File[] files = subtitleDir.listFiles((dir, name) -> 
            name.startsWith(videoId) && name.endsWith(".vtt"));
        
        if (files != null && files.length > 0) {
            // 优先返回包含目标语言代码的文件
            for (File file : files) {
                if (file.getName().contains(langCode) || file.getName().contains(language)) {
                    return file;
                }
            }
            // 如果没有匹配的，返回第一个
            return files[0];
        }
        
        return null;
    }
    
    /**
     * 查找任何字幕文件（不指定语言）
     */
    private File findAnySubtitleFile(String videoId) {
        File subtitleDir = new File(SUBTITLE_DIR);
        if (!subtitleDir.exists()) {
            return null;
        }
        
        File[] files = subtitleDir.listFiles((dir, name) -> 
            name.startsWith(videoId) && name.endsWith(".vtt"));
        
        if (files != null && files.length > 0) {
            // 返回第一个找到的文件
            return files[0];
        }
        
        return null;
    }

    /**
     * 获取语言显示名称
     */
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
        
        // 提取视频ID（从文件路径中）
        String videoIdStr = new File(filePath).getName().split("\\.")[0];
        File subtitleDir = new File(SUBTITLE_DIR);
        
        // 尝试多个可能的文件名格式
        Path path = null;
        List<String> triedPaths = new ArrayList<>();
        
        // 1. 尝试原始路径
        path = Paths.get(filePath);
        triedPaths.add(filePath);
        if (Files.exists(path)) {
            log.info("找到字幕文件: {}", filePath);
        } else {
            // 2. 尝试带语言代码的文件名（简化版）
            if (language != null) {
                String langCode = getLanguageCode(language);
                String altPath = SUBTITLE_DIR + videoIdStr + "." + langCode + ".vtt";
                path = Paths.get(altPath);
                triedPaths.add(altPath);
                if (Files.exists(path)) {
                    log.info("找到字幕文件: {}", altPath);
                }
            }
            
            // 3. 如果还没找到，列出目录中所有匹配的文件
            if (!Files.exists(path) && subtitleDir.exists()) {
                File[] files = subtitleDir.listFiles((dir, name) -> 
                    name.startsWith(videoIdStr) && name.endsWith(".vtt"));
                
                if (files != null && files.length > 0) {
                    // 使用第一个找到的文件
                    path = files[0].toPath();
                    log.info("找到字幕文件（自动匹配）: {}", path);
                }
            }
            
            // 4. 尝试常见语言后缀
        if (!Files.exists(path)) {
                String[] commonLangs = {"en", "zh", "ja", "ko", "zh-Hans", "zh-Hant", "en-US", "zh-CN", "zh-TW"};
                for (String lang : commonLangs) {
                    String altPath = SUBTITLE_DIR + videoIdStr + "." + lang + ".vtt";
                    Path alt = Paths.get(altPath);
                    triedPaths.add(altPath);
                    if (Files.exists(alt)) {
                        path = alt;
                        log.info("找到字幕文件: {}", altPath);
                        break;
                    }
                }
            }
            
            // 5. 尝试不带语言后缀的文件名（yt-dlp 可能生成这种格式）
        if (!Files.exists(path)) {
                String altPath = SUBTITLE_DIR + videoIdStr + ".vtt";
                Path alt = Paths.get(altPath);
                triedPaths.add(altPath);
                if (Files.exists(alt)) {
                    path = alt;
                    log.info("找到字幕文件: {}", altPath);
                }
            }
        }
        
        if (path == null || !Files.exists(path)) {
            // 列出所有尝试过的路径
            log.error("字幕文件未找到，尝试过的路径: {}", String.join(", ", triedPaths));
            
            // 列出目录中的所有文件
            if (subtitleDir.exists()) {
                File[] allFiles = subtitleDir.listFiles();
                if (allFiles != null) {
                    log.error("字幕目录中的所有文件: {}", 
                        Arrays.toString(Arrays.stream(allFiles).map(File::getName).toArray()));
                }
            }
            
            throw new RuntimeException("Subtitle file not found: " + filePath + 
                (language != null ? " (language: " + language + ")" : "") +
                ". Tried paths: " + String.join(", ", triedPaths));
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

