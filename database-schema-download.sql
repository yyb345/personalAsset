-- 视频下载任务表
CREATE TABLE IF NOT EXISTS video_download_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    youtube_video_id BIGINT,
    video_id VARCHAR(50),
    download_type VARCHAR(20),
    format_id VARCHAR(50),
    quality VARCHAR(50),
    status VARCHAR(20) DEFAULT 'INIT',
    progress INT DEFAULT 0,
    progress_message VARCHAR(500),
    download_speed VARCHAR(50),
    downloaded_bytes BIGINT,
    total_bytes BIGINT,
    output_file VARCHAR(500),
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_by BIGINT,
    INDEX idx_youtube_video (youtube_video_id),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 如果使用 SQLite (开发环境)
-- CREATE TABLE IF NOT EXISTS video_download_tasks (
--     id INTEGER PRIMARY KEY AUTOINCREMENT,
--     youtube_video_id INTEGER,
--     video_id TEXT,
--     download_type TEXT,
--     format_id TEXT,
--     quality TEXT,
--     status TEXT DEFAULT 'INIT',
--     progress INTEGER DEFAULT 0,
--     progress_message TEXT,
--     download_speed TEXT,
--     downloaded_bytes INTEGER,
--     total_bytes INTEGER,
--     output_file TEXT,
--     error_message TEXT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     started_at TIMESTAMP,
--     completed_at TIMESTAMP,
--     created_by INTEGER
-- );

