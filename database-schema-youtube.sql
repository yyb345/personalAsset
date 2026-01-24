-- YouTube 视频跟读功能 - 数据库表结构
-- 注意：使用 JPA 时会自动创建这些表，此文件仅供参考

-- ========================================
-- 1. YouTube 视频表
-- ========================================
CREATE TABLE IF NOT EXISTS youtube_videos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_id VARCHAR(20) NOT NULL UNIQUE,           -- YouTube 视频 ID
    source_url VARCHAR(500) NOT NULL,               -- 完整 URL
    title VARCHAR(500) NOT NULL,                    -- 视频标题
    description TEXT,                               -- 视频描述
    duration INTEGER NOT NULL,                      -- 时长（秒）
    channel VARCHAR(100),                           -- 频道名称
    has_subtitle BOOLEAN DEFAULT 0,                 -- 是否有字幕
    subtitle_language VARCHAR(10),                  -- 字幕语言
    thumbnail_url VARCHAR(500),                     -- 缩略图 URL
    status VARCHAR(20) NOT NULL DEFAULT 'parsing',  -- parsing/completed/failed
    sentence_count INTEGER,                         -- 生成的句子数
    difficulty_level VARCHAR(20),                   -- auto/easy/medium/hard
    created_by INTEGER,                             -- 创建者 ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    error_message VARCHAR(1000)                     -- 错误信息
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_youtube_videos_video_id ON youtube_videos(video_id);
CREATE INDEX IF NOT EXISTS idx_youtube_videos_status ON youtube_videos(status);
CREATE INDEX IF NOT EXISTS idx_youtube_videos_created_by ON youtube_videos(created_by);

-- ========================================
-- 2. 字幕片段表
-- ========================================
CREATE TABLE IF NOT EXISTS subtitle_segments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_id INTEGER NOT NULL,                      -- 关联 youtube_videos.id
    start_time REAL NOT NULL,                       -- 开始时间（秒）
    end_time REAL NOT NULL,                         -- 结束时间（秒）
    raw_text VARCHAR(2000) NOT NULL,                -- 原始字幕文本
    clean_text VARCHAR(2000),                       -- 清洗后文本
    segment_order INTEGER NOT NULL,                 -- 顺序
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES youtube_videos(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_subtitle_segments_video_id ON subtitle_segments(video_id);
CREATE INDEX IF NOT EXISTS idx_subtitle_segments_order ON subtitle_segments(video_id, segment_order);

-- ========================================
-- 3. 扩展现有的 follow_read_sentences 表
-- ========================================
-- 添加 YouTube 相关字段（如果表已存在，需要手动执行 ALTER TABLE）

-- 如果是新表，使用以下完整定义：
CREATE TABLE IF NOT EXISTS follow_read_sentences (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    text VARCHAR(1000) NOT NULL,                    -- 句子文本
    phonetic VARCHAR(2000) NOT NULL,                -- 音标
    audio_url VARCHAR(500) NOT NULL,                -- 音频 URL
    difficulty VARCHAR(20) NOT NULL,                -- easy/medium/hard
    category VARCHAR(100),                          -- VOA/TED/Daily/YouTube
    
    -- YouTube 相关字段（新增）
    youtube_video_id INTEGER,                       -- 关联 youtube_videos.id
    start_time REAL,                                -- 在视频中的开始时间
    end_time REAL,                                  -- 在视频中的结束时间
    sentence_order INTEGER,                         -- 在视频中的顺序
    video_url VARCHAR(500),                         -- YouTube 视频 URL
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (youtube_video_id) REFERENCES youtube_videos(id) ON DELETE CASCADE
);

-- 如果表已存在，执行以下 ALTER TABLE 语句：
/*
ALTER TABLE follow_read_sentences ADD COLUMN youtube_video_id INTEGER;
ALTER TABLE follow_read_sentences ADD COLUMN start_time REAL;
ALTER TABLE follow_read_sentences ADD COLUMN end_time REAL;
ALTER TABLE follow_read_sentences ADD COLUMN sentence_order INTEGER;
ALTER TABLE follow_read_sentences ADD COLUMN video_url VARCHAR(500);
*/

-- 索引
CREATE INDEX IF NOT EXISTS idx_follow_read_sentences_youtube_video_id 
    ON follow_read_sentences(youtube_video_id);
CREATE INDEX IF NOT EXISTS idx_follow_read_sentences_difficulty 
    ON follow_read_sentences(difficulty);
CREATE INDEX IF NOT EXISTS idx_follow_read_sentences_category 
    ON follow_read_sentences(category);

-- ========================================
-- 4. 数据统计视图（可选）
-- ========================================
CREATE VIEW IF NOT EXISTS youtube_video_stats AS
SELECT 
    yv.id,
    yv.video_id,
    yv.title,
    yv.channel,
    yv.duration,
    yv.status,
    yv.sentence_count,
    COUNT(DISTINCT frs.id) as actual_sentence_count,
    COUNT(DISTINCT ss.id) as segment_count,
    AVG(frs.end_time - frs.start_time) as avg_sentence_duration
FROM youtube_videos yv
LEFT JOIN follow_read_sentences frs ON yv.id = frs.youtube_video_id
LEFT JOIN subtitle_segments ss ON yv.id = ss.video_id
GROUP BY yv.id;

-- ========================================
-- 5. 示例查询
-- ========================================

-- 查询用户的所有视频
/*
SELECT * FROM youtube_videos 
WHERE created_by = ? 
ORDER BY created_at DESC;
*/

-- 查询视频的所有学习句子（按时间顺序）
/*
SELECT * FROM follow_read_sentences 
WHERE youtube_video_id = ? 
ORDER BY sentence_order;
*/

-- 查询解析中的视频
/*
SELECT * FROM youtube_videos 
WHERE status = 'parsing' 
ORDER BY created_at DESC;
*/

-- 统计用户的学习进度
/*
SELECT 
    yv.title,
    COUNT(DISTINCT frt.id) as completed_tasks,
    yv.sentence_count as total_sentences,
    ROUND(COUNT(DISTINCT frt.id) * 100.0 / yv.sentence_count, 2) as completion_rate
FROM youtube_videos yv
LEFT JOIN follow_read_sentences frs ON yv.id = frs.youtube_video_id
LEFT JOIN follow_read_tasks frt ON frs.id = frt.sentence_id AND frt.status = 'completed'
WHERE yv.created_by = ?
GROUP BY yv.id;
*/

-- ========================================
-- 6. 数据清理（可选）
-- ========================================

-- 删除失败的解析任务（7 天前）
/*
DELETE FROM youtube_videos 
WHERE status = 'failed' 
AND created_at < datetime('now', '-7 days');
*/

-- 删除孤立的字幕片段
/*
DELETE FROM subtitle_segments 
WHERE video_id NOT IN (SELECT id FROM youtube_videos);
*/

-- ========================================
-- 7. 性能优化建议
-- ========================================

-- 定期分析表以优化查询性能
/*
ANALYZE youtube_videos;
ANALYZE subtitle_segments;
ANALYZE follow_read_sentences;
*/

-- 如果数据量很大，可以考虑添加额外的复合索引
/*
CREATE INDEX idx_follow_read_sentences_video_order 
    ON follow_read_sentences(youtube_video_id, sentence_order);
*/

