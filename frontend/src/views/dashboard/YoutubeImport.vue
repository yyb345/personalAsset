<template>
  <div class="youtube-import-container">
    <div class="header">
      <h1>{{ $t('youtube.title') }}</h1>
      <p class="subtitle">{{ $t('youtube.subtitle') }}</p>
    </div>

    <!-- YouTube URL 输入区 -->
    <div v-if="!selectedVideo" class="import-section">
      <div v-if="isDebugMode" class="url-input-card">
        <p class="hint">{{ $t('youtube.pasteHint') }}</p>

        <div class="input-group">
          <div class="command-input-wrapper">
            <span class="command-prefix">▶</span>
            <input
              v-model="youtubeUrl"
              type="text"
              placeholder="youtube.com/watch?v=..."
              class="url-input"
              @keyup.enter="addVideoToLibrary"
            />
          </div>
        </div>


        <button
          class="parse-btn"
          @click="addVideoToLibrary"
          :disabled="!youtubeUrl || isAdding"
        >
          <span v-if="!isAdding">{{ $t('youtube.generatePractice') }}</span>
          <span v-else class="ai-processing">
            <span class="processing-text">
              <span class="stage">{{ $t('youtube.analyzing') }}</span>
              <span class="separator">·</span>
              <span class="stage">{{ $t('youtube.extracting') }}</span>
              <span class="separator">·</span>
              <span class="stage">{{ $t('youtube.generating') }}</span>
            </span>
          </span>
        </button>

        <!-- 系统状态检查 -->
        <div v-if="!systemReady" class="warning-box">
          <p>⚠️ {{ $t('youtube.systemNotReady') }}</p>
          <p class="hint-text">{{ $t('youtube.installHint') }}: <code>pip install yt-dlp</code></p>
        </div>
      </div>

      <!-- 字幕搜索区域 -->
      <div class="subtitle-search-section">
        <div class="search-header">
          <h2>{{ $t('youtube.search.title') }}</h2>
        </div>
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
          <input
            v-model="searchKeyword"
            type="text"
            :placeholder="$t('youtube.search.placeholder')"
            class="search-input"
            @keyup.enter="performSearch"
          />
          <button
            v-if="searchKeyword"
            class="clear-search-btn"
            @click="clearSearch"
          >×</button>
          <button
            class="search-btn"
            @click="performSearch"
            :disabled="!searchKeyword || isSearching"
          >
            <span v-if="!isSearching">{{ $t('youtube.search.search') }}</span>
            <span v-else>{{ $t('youtube.search.searching') }}</span>
          </button>
        </div>

        <!-- 搜索结果 -->
        <div v-if="searchResults.length > 0 || hasSearched" class="search-results">
          <div class="search-results-header">
            <span>{{ $t('youtube.search.results', { count: searchTotal }) }}</span>
            <button class="close-results-btn" @click="clearSearch">{{ $t('youtube.search.close') }}</button>
          </div>

          <div v-if="searchResults.length === 0 && hasSearched" class="no-results">
            <p>{{ $t('youtube.search.noResults') }}</p>
          </div>

          <div v-else class="search-result-list">
            <div
              v-for="result in searchResults"
              :key="result.videoId"
              class="search-result-card"
            >
              <div class="result-thumbnail" @click="goToVideo(result.videoId)">
                <img v-if="result.thumbnailUrl" :src="result.thumbnailUrl" alt="thumbnail" />
                <div v-else class="thumbnail-placeholder">📹</div>
              </div>
              <div class="result-content">
                <h3 class="result-title" @click="goToVideo(result.videoId)">{{ result.title }}</h3>
                <div class="result-meta">
                  <span class="channel">{{ result.channel }}</span>
                  <span class="duration">{{ formatDuration(result.duration) }}</span>
                  <span v-if="result.language" class="language">{{ result.language }}</span>
                </div>
                <!-- 匹配的字幕片段 -->
                <div v-if="result.matchedSegments && result.matchedSegments.length > 0" class="matched-segments">
                  <div
                    v-for="(seg, idx) in result.matchedSegments.slice(0, 3)"
                    :key="idx"
                    class="segment-item"
                    @click="goToVideoAtTime(result.videoId, seg.startTime)"
                  >
                    <span class="segment-time">{{ formatTime(seg.startTime) }}</span>
                    <span class="segment-text" v-html="seg.highlightedText || seg.text"></span>
                  </div>
                </div>
                <!-- 高亮摘要 -->
                <div v-else-if="result.highlights && result.highlights.length > 0" class="highlights">
                  <div
                    v-for="(hl, idx) in result.highlights.slice(0, 2)"
                    :key="idx"
                    class="highlight-item"
                    v-html="hl"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 搜索分页 -->
          <div v-if="searchTotalPages > 1" class="search-pagination">
            <button
              class="page-btn"
              @click="searchPreviousPage"
              :disabled="searchPage === 0"
            >{{ $t('youtube.pagination.previous') }}</button>
            <span class="page-info">{{ searchPage + 1 }} / {{ searchTotalPages }}</span>
            <button
              class="page-btn"
              @click="searchNextPage"
              :disabled="searchPage >= searchTotalPages - 1"
            >{{ $t('youtube.pagination.next') }}</button>
          </div>
        </div>
      </div>

      <!-- 我的视频列表 -->
      <div class="my-videos-section">
        <h2>{{ $t('youtube.myLibrary') }} <span v-if="totalElements > 0" class="total-count">({{ totalElements }} {{ $t('youtube.videoCount', { count: '' }).replace('{count}', '').trim() }})</span></h2>
        <div v-if="myVideos.length === 0" class="empty-state">
          <p>{{ $t('youtube.noVideos') }}</p>
        </div>
        <div v-else class="video-list">
          <div
            v-for="video in myVideos"
            :key="video.id"
            :class="['video-card', { 'is-pinned': video.pinned }]"
            @mouseenter="onCardHover(video, $event)"
            @mouseleave="onCardLeave"
          >
            <div class="video-thumbnail" @click="viewVideoDetails(video)">
              <img v-if="video.thumbnailUrl" :src="video.thumbnailUrl" alt="thumbnail" />
              <div v-else class="thumbnail-placeholder">📹</div>
            </div>
            <div class="video-info" @click="viewVideoDetails(video)">
              <h3 class="video-title">
                <span v-if="video.pinned" class="pin-badge">📌</span>
                {{ video.title || 'Loading...' }}
              </h3>
              <div class="video-meta">
                <span class="channel">{{ video.channel }}</span>
                <span class="duration">{{ formatDuration(video.duration) }}</span>
              </div>
              <div class="video-status-row">
                <div class="video-status">
                  <span :class="['status-badge', video.status]">
                    {{ getStatusLabel(video.status) }}
                  </span>
                  <span v-if="video.sentenceCount" class="sentence-count">
                    {{ video.sentenceCount }} sentences
                  </span>
                </div>
                <div class="video-actions">
                  <button 
                    class="action-btn parse-btn" 
                    @click.stop="parseSubtitles(video)"
                    title="Parse Subtitles"
                    v-if="video.status === 'added'"
                    :disabled="video.status === 'parsing'">
                    <Pencil :size="16" :stroke-width="2" />
                    <span class="kbd">P</span>
                  </button>
                  <button
                    class="action-btn read-btn"
                    @click.stop="openTranscribeRead(video)"
                    title="Read Transcript"
                    v-if="video.status === 'completed'">
                    <BookOpen :size="16" :stroke-width="2" />
                    <span class="kbd">R</span>
                  </button>
                  <button
                    class="action-btn study-btn"
                    @click.stop="openStudyWorkspace(video)"
                    title="Study Workspace">
                    <NotebookPen :size="16" :stroke-width="2" />
                    <span class="kbd">S</span>
                  </button>
                  <button
                    class="action-btn download-btn"
                    @click.stop="showDownloadOptions(video)"
                    title="Download Video"
                    :disabled="video.status !== 'completed' && video.status !== 'added'">
                    <Download :size="16" :stroke-width="2" />
                    <span class="kbd">D</span>
                  </button>
                  <button
                    :class="['action-btn', 'pin-btn', { 'is-active': video.pinned }]"
                    @click.stop="togglePin(video)"
                    :title="video.pinned ? 'Unpin' : 'Pin to top'">
                    <Pin :size="16" :stroke-width="2" />
                  </button>
                  <button
                    class="action-btn delete-btn"
                    @click.stop="confirmDeleteVideo(video)"
                    title="Delete Video">
                    <Trash2 :size="16" :stroke-width="2" />
                    <span class="kbd">⌫</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页控件 -->
        <div v-if="totalPages > 1" class="pagination">
          <button
            class="page-btn"
            @click="previousPage"
            :disabled="!hasPrevious"
          >
            {{ $t('youtube.pagination.previous') }}
          </button>

          <div class="page-numbers">
            <button
              v-for="page in visiblePages"
              :key="page"
              :class="['page-num', { active: page === currentPage }]"
              @click="goToPage(page)"
            >
              {{ page + 1 }}
            </button>
          </div>

          <button
            class="page-btn"
            @click="nextPage"
            :disabled="!hasNext"
          >
            {{ $t('youtube.pagination.next') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 解析进度展示 -->
    <div v-if="parsingVideo" class="parsing-progress">
      <div class="progress-card">
        <h2>{{ $t('youtube.processing') }}</h2>
        <div class="progress-info">
          <p><strong>{{ $t('youtube.videoId') }}:</strong> {{ parsingVideo.videoId }}</p>
          <p><strong>{{ $t('youtube.status.added') }}:</strong> {{ getStatusLabel(parsingVideo.status) }}</p>
        </div>

        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <p class="progress-text">{{ progressText }}</p>

        <button class="cancel-btn" @click="cancelParsing">{{ $t('common.cancel') }}</button>
      </div>
    </div>

    <!-- 视频详情和句子列表 -->
    <div v-if="selectedVideo" class="video-details">
      <button class="back-btn" @click="backToList">← {{ $t('common.back') }}</button>
      
      <div class="video-header">
        <div class="video-thumbnail-large">
          <img v-if="selectedVideo.thumbnailUrl" :src="selectedVideo.thumbnailUrl" alt="thumbnail" />
        </div>
        <div class="video-header-info">
          <h1>{{ selectedVideo.title }}</h1>
          <div class="meta">
            <span>📺 {{ selectedVideo.channel }}</span>
            <span>⏱️ {{ formatDuration(selectedVideo.duration) }}</span>
            <span>📝 {{ selectedVideo.sentenceCount }} sentences</span>
          </div>
          <p class="description">{{ selectedVideo.description?.substring(0, 200) }}...</p>
        </div>
      </div>

      <!-- 句子列表 -->
      <div class="sentences-section">
        <h2>{{ $t('youtube.practice.title') }}</h2>
        <div v-if="!videoSentences || videoSentences.length === 0" class="empty-state">
          <p>{{ $t('youtube.practice.noSentences') }}</p>
        </div>
        <div v-else class="sentence-list">
          <div 
            v-for="(sentence, index) in videoSentences" 
            :key="sentence.id"
            class="sentence-item-wrapper"
          >
            <!-- 句子卡片 -->
            <div 
              :class="['sentence-item', { 'active': practicingSentence?.id === sentence.id }]"
              @click="togglePractice(sentence)"
            >
              <div class="sentence-number">{{ index + 1 }}</div>
              <div class="sentence-content">
                <div class="sentence-text">{{ sentence.text }}</div>
                <div class="sentence-meta">
                  <span class="timestamp">
                    {{ formatTime(sentence.startTime) }} - {{ formatTime(sentence.endTime) }}
                  </span>
                  <span :class="['difficulty-badge', sentence.difficulty]">
                    {{ getDifficultyLabel(sentence.difficulty) }}
                  </span>
                </div>
              </div>
              <button class="practice-btn" @click.stop="togglePractice(sentence)">
                {{ practicingSentence?.id === sentence.id ? $t('youtube.practice.collapse') : $t('youtube.practice.practice') }}
              </button>
            </div>

            <transition name="slide-down">
              <div v-if="practicingSentence?.id === sentence.id" class="practice-panel">
                <!-- YouTube播放器 -->
                <div class="practice-player">
                  <h3>{{ $t('youtube.practice.referenceAudio') }}</h3>
                  <div :id="'youtube-practice-' + sentence.id" class="youtube-practice-container"></div>
                  <button
                    class="play-segment-btn"
                    @click="playYoutubeSegment"
                    :disabled="!youtubePlayerReady"
                  >
                    {{ isPlayingStandard ? $t('youtube.practice.stop') : $t('youtube.practice.play') }}
                  </button>
                  <p class="time-hint">{{ formatTime(sentence.startTime) }} - {{ formatTime(sentence.endTime) }}</p>
                </div>

                <!-- 录音区域 -->
                <div class="practice-recording">
                  <h3>{{ $t('youtube.practice.yourRecording') }}</h3>

                  <!-- 未录音状态 -->
                  <div v-if="!isRecording && !userAudioUrl" class="record-ready">
                    <p>{{ $t('youtube.practice.readyToPractice') }}</p>
                    <button class="record-btn" @click="startRecording" :disabled="!canRecord">
                      {{ $t('youtube.practice.startRecording') }}
                    </button>
                  </div>

                  <!-- 录音中 -->
                  <div v-if="isRecording" class="recording-active">
                    <div class="recording-indicator">
                      <span class="pulse"></span>
                      {{ $t('youtube.practice.recording') }}
                    </div>
                    <button class="stop-record-btn" @click="stopRecording">
                      {{ $t('youtube.practice.stopRecording') }}
                    </button>
                  </div>

                  <!-- 已录音 -->
                  <div v-if="userAudioUrl" class="recorded">
                    <audio :src="userAudioUrl" controls></audio>
                    <button class="rerecord-btn" @click="reRecord">
                      {{ $t('youtube.practice.reRecord') }}
                    </button>
                  </div>

                  <!-- 评分结果 -->
                  <div v-if="taskResult && taskResult.task.status === 'completed'" class="score-result">
                    <div class="overall-score">
                      <div class="score-circle" :class="getScoreClass(taskResult.task.overallScore)">
                        <span class="score-value">{{ taskResult.task.overallScore }}</span>
                        <span class="score-label">{{ $t('youtube.practice.score') }}</span>
                      </div>
                    </div>

                    <div class="score-details">
                      <div class="score-bar-item">
                        <span>{{ $t('youtube.practice.pronunciation') }}</span>
                        <div class="bar">
                          <div class="fill" :style="{ width: taskResult.task.pronunciationScore + '%' }"></div>
                        </div>
                        <span>{{ taskResult.task.pronunciationScore }}</span>
                      </div>
                      <div class="score-bar-item">
                        <span>{{ $t('youtube.practice.fluency') }}</span>
                        <div class="bar">
                          <div class="fill fluency" :style="{ width: taskResult.task.fluencyScore + '%' }"></div>
                        </div>
                        <span>{{ taskResult.task.fluencyScore }}</span>
                      </div>
                      <div class="score-bar-item">
                        <span>{{ $t('youtube.practice.intonation') }}</span>
                        <div class="bar">
                          <div class="fill intonation" :style="{ width: taskResult.task.intonationScore + '%' }"></div>
                        </div>
                        <span>{{ taskResult.task.intonationScore }}</span>
                      </div>
                    </div>

                    <!-- 单词级别详细反馈 -->
                    <div class="word-feedback-section">
                      <h3>{{ $t('youtube.practice.wordFeedback') }}</h3>

                      <!-- 单词卡片列表 -->
                      <div class="words-container">
                        <div
                          v-for="(result, index) in taskResult.results"
                          :key="index"
                          :class="['word-item', result.status]"
                          :title="result.feedback"
                        >
                          <span class="word-text">{{ result.word }}</span>
                          <span class="word-score">{{ result.score }}</span>
                        </div>
                      </div>

                      <!-- 发音提示说明 -->
                      <div class="legend">
                        <span class="legend-item">
                          <span class="dot correct"></span> {{ $t('youtube.practice.correct') }}
                        </span>
                        <span class="legend-item">
                          <span class="dot incorrect"></span> {{ $t('youtube.practice.needImprovement') }}
                        </span>
                      </div>

                      <!-- 错误单词详细提示 -->
                      <div v-if="incorrectWords.length > 0" class="error-tips">
                        <h4>{{ $t('youtube.practice.suggestions') }}</h4>
                        <div
                          v-for="result in incorrectWords"
                          :key="result.wordPosition"
                          class="error-tip"
                        >
                          <span class="error-word">{{ result.word }}</span>
                          <span class="error-feedback">{{ result.feedback }}</span>
                        </div>
                      </div>

                      <div v-else class="all-correct">
                        <p>{{ $t('youtube.practice.allCorrect') }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </div>

    <!-- 下载选项弹窗 -->
    <div v-if="showDownloadDialog" class="modal-overlay" @click="closeDownloadDialog">
      <div class="download-dialog" @click.stop>
      <div class="dialog-header">
        <h2>{{ $t('youtube.download.title') }}</h2>
          <button class="close-btn" @click="closeDownloadDialog">×</button>
        </div>

        <div class="dialog-content">
          <h3>{{ downloadingVideo?.title }}</h3>

          <div class="quality-selector">
            <label>{{ $t('youtube.download.quality') }}</label>
            <select v-model="selectedQuality" class="quality-select">
              <option value="best">{{ $t('youtube.download.best') }}</option>
              <option value="4k">{{ $t('youtube.download.4k') }}</option>
              <option value="2k">{{ $t('youtube.download.2k') }}</option>
              <option value="1080p">{{ $t('youtube.download.1080p') }}</option>
              <option value="720p">{{ $t('youtube.download.720p') }}</option>
              <option value="480p">{{ $t('youtube.download.480p') }}</option>
            </select>
            <p class="quality-hint">{{ $t('youtube.download.qualityHint') }}</p>
          </div>

          <div class="download-options">
            <div class="option-card" @click="downloadWithQuality('video')">
              <div class="option-icon">🎬</div>
              <div class="option-info">
                <h4>{{ $t('youtube.download.downloadVideo') }}</h4>
                <p>{{ getQualityDescription(selectedQuality) }}</p>
              </div>
            </div>

            <div class="option-card" @click="quickDownload('audio')">
              <div class="option-icon">🎵</div>
              <div class="option-info">
                <h4>{{ $t('youtube.download.audioOnly') }}</h4>
                <p>{{ $t('youtube.download.extractMp3') }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下载任务列表弹窗 -->
    <div v-if="showDownloadTasks" class="modal-overlay" @click="showDownloadTasks = false">
      <div class="download-tasks-dialog" @click.stop>
        <div class="dialog-header">
          <h2>{{ $t('youtube.download.myDownloads') }}</h2>
          <button class="close-btn" @click="showDownloadTasks = false">×</button>
        </div>

        <div class="dialog-content">
          <div v-if="downloadTasks.length === 0" class="empty-state">
            <p>{{ $t('youtube.download.noTasks') }}</p>
          </div>
          
          <div v-else class="task-list">
            <div 
              v-for="task in downloadTasks" 
              :key="task.id"
              class="task-item"
            >
              <div class="task-info">
                <h4>{{ getVideoTitle(task.youtubeVideoId) }}</h4>
                <div class="task-meta">
                  <span :class="['status-badge', task.status.toLowerCase()]">
                    {{ getDownloadStatusLabel(task.status) }}
                  </span>
                  <span class="task-type">{{ getDownloadTypeLabel(task.downloadType) }}</span>
                  <span v-if="task.quality" class="quality">{{ task.quality }}</span>
                </div>
                <div v-if="task.status === 'DOWNLOADING' || task.status === 'QUEUED'" class="progress-bar">
                  <div class="progress-fill" :style="{ width: task.progress + '%' }"></div>
                </div>
                <p class="progress-text">{{ task.progressMessage }}</p>
                <p v-if="task.downloadSpeed" class="speed">{{ task.downloadSpeed }}</p>
              </div>
              
              <div class="task-actions">
                <button 
                  v-if="task.status === 'SUCCESS'" 
                  class="btn-download-file"
                  @click="downloadFile(task.id)">
                  💾 Download
                </button>
                <button 
                  class="btn-delete-task"
                  @click="deleteDownloadTask(task.id)">
                  🗑️
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 浮动下载按钮 -->
    <button 
      v-if="!selectedVideo" 
      class="fab-download" 
      @click="showDownloadTasks = true"
      title="查看下载任务">
      📥
      <span v-if="activeDownloadCount > 0" class="download-badge">{{ activeDownloadCount }}</span>
    </button>

    <!-- 笔记悬浮预览（Teleport 到 body 避免被裁切） -->
    <Teleport to="body">
      <transition name="note-fade">
        <div
          v-if="notePreview.visible"
          class="note-preview-card"
          :style="notePreview.style"
          @mouseenter="notePreview.hovering = true"
          @mouseleave="onNotePreviewLeave"
        >
          <div class="note-preview-header">
            <span class="note-preview-icon">📝</span>
            <span class="note-preview-count">{{ notePreview.notes.length }} note{{ notePreview.notes.length > 1 ? 's' : '' }}</span>
          </div>
          <div v-for="note in notePreview.notes.slice(0, 3)" :key="note.id" class="note-preview-item">
            <div class="note-preview-title">{{ note.title || 'Untitled' }}</div>
            <div class="note-preview-text" v-html="note.contentHtml"></div>
            <div class="note-preview-date">{{ formatNoteDate(note.updatedAt) }}</div>
          </div>
          <div v-if="notePreview.notes.length > 3" class="note-preview-more">
            +{{ notePreview.notes.length - 3 }} more
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script>
import axios from '../../utils/axios';
import { Pencil, Download, Trash2, BookOpen, NotebookPen, Pin } from 'lucide-vue-next';
import { useI18n } from 'vue-i18n';

export default {
  name: 'YoutubeImport',
  components: {
    Pencil,
    Download,
    Trash2,
    BookOpen,
    NotebookPen,
    Pin
  },
  setup() {
    const { t } = useI18n();
    return { t };
  },
  data() {
    return {
      youtubeUrl: '',
      difficulty: 'auto', // 固定使用 auto，让 AI 自动判断
      systemReady: true,
      isDebugMode: false, // 只在 URL 包含 ?debug=true 时显示 url-input-card
      isAdding: false,
      isParsing: false,
      parsingVideo: null,
      progressPercent: 0,
      progressText: 'Preparing...',
      
      myVideos: [],
      selectedVideo: null,
      videoSentences: [],

      // 分页相关
      currentPage: 0,
      totalPages: 0,
      pageSize: 9,
      totalElements: 0,
      hasNext: false,
      hasPrevious: false,
      
      pollInterval: null,
      videoListPollInterval: null, // 新增：视频列表轮询定时器
      
      // 练习相关
      practicingSentence: null,
      youtubePlayer: null,
      youtubePlayerReady: false,
      isPlayingStandard: false,
      playbackTimer: null,
      
      // 录音相关
      mediaRecorder: null,
      audioChunks: [],
      isRecording: false,
      canRecord: false,
      userAudioUrl: null,
      
      // 任务和结果
      currentTask: null,
      taskResult: null,
      taskPollInterval: null,
      
      // 下载相关
      showDownloadDialog: false,
      downloadingVideo: null,
      downloadTasks: [],
      showDownloadTasks: false,
      downloadPollInterval: null,
      activeDownloadCount: 0,
      selectedQuality: 'best',
      sseSource: null,
      sseConnected: false,

      // 笔记悬浮预览
      notePreview: {
        visible: false,
        hovering: false,
        notes: [],
        style: {}
      },
      noteHoverTimer: null,
      noteCache: {},

      // 字幕搜索相关
      searchKeyword: '',
      searchResults: [],
      searchTotal: 0,
      searchPage: 0,
      searchPageSize: 10,
      searchTotalPages: 0,
      isSearching: false,
      hasSearched: false
    };
  },
  computed: {
    incorrectWords() {
      if (!this.taskResult || !this.taskResult.results) return [];
      return this.taskResult.results.filter(r => r.status === 'incorrect');
    },
    visiblePages() {
      const pages = [];
      const maxVisible = 5;
      let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
      let end = Math.min(this.totalPages, start + maxVisible);

      if (end - start < maxVisible) {
        start = Math.max(0, end - maxVisible);
      }

      for (let i = start; i < end; i++) {
        pages.push(i);
      }
      return pages;
    }
  },
  mounted() {
    // 检查 URL 参数是否包含 debug=true
    this.isDebugMode = this.$route.query.debug === 'true';
    this.checkSystemStatus();
    this.loadMyVideos();
    this.checkMicrophonePermission();
    this.loadYoutubeAPI();
    this.loadDownloadTasks();
    this.connectSSE();
    this.startVideoListPolling();
  },
  beforeUnmount() {
    this.clearPolling();
    if (this.noteHoverTimer) {
      clearTimeout(this.noteHoverTimer);
    }
    if (this.taskPollInterval) {
      clearInterval(this.taskPollInterval);
    }
    if (this.playbackTimer) {
      clearTimeout(this.playbackTimer);
    }
    if (this.youtubePlayer) {
      this.youtubePlayer.destroy();
    }
    if (this.downloadPollInterval) {
      clearInterval(this.downloadPollInterval);
    }
    if (this.sseSource) {
      this.sseSource.close();
      this.sseSource = null;
    }
    if (this.videoListPollInterval) {
      clearInterval(this.videoListPollInterval);
    }
  },
  methods: {
    async checkSystemStatus() {
      try {
        const response = await axios.get('/api/youtube/status');
        this.systemReady = response.data.ready;
      } catch (error) {
        console.error('检查系统状态失败:', error);
        this.systemReady = false;
      }
    },
    
    async loadMyVideos(page = this.currentPage) {
      try {
        const response = await axios.get('/api/youtube/videos', {
          params: {
            page: page,
            size: this.pageSize
          }
        });
        const data = response.data;
        // 兼容两种返回格式：分页格式 { content, totalElements, ... } 或 直接数组
        const list = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);
        this.myVideos = list.map((v) => this.normalizeVideoItem(v));
        this.currentPage = typeof data.currentPage === 'number' ? data.currentPage : (data.number ?? 0);
        this.totalPages = typeof data.totalPages === 'number' ? data.totalPages : (list.length > 0 ? 1 : 0);
        this.totalElements = typeof data.totalElements === 'number' ? data.totalElements : list.length;
        this.hasNext = Boolean(data.hasNext);
        this.hasPrevious = Boolean(data.hasPrevious);
      } catch (error) {
        console.error('加载视频列表失败:', error);
        this.myVideos = [];
      }
    },
    normalizeVideoItem(v) {
      if (!v || typeof v !== 'object') return {};
      return {
        id: v.id,
        videoId: v.videoId,
        title: v.title ?? '',
        channel: v.channel ?? '',
        duration: v.duration ?? 0,
        thumbnailUrl: v.thumbnailUrl ?? v.thumbnail_url ?? '',
        status: v.status ?? 'added',
        sentenceCount: v.sentenceCount ?? v.sentence_count ?? 0,
        pinned: v.pinned ?? false
      };
    },

    goToPage(page) {
      if (page >= 0 && page < this.totalPages) {
        this.loadMyVideos(page);
      }
    },

    previousPage() {
      if (this.hasPrevious) {
        this.goToPage(this.currentPage - 1);
      }
    },

    nextPage() {
      if (this.hasNext) {
        this.goToPage(this.currentPage + 1);
      }
    },
    
    startVideoListPolling() {
      // 每3秒轮询一次视频列表，以更新状态
      this.videoListPollInterval = setInterval(() => {
        this.loadMyVideos(this.currentPage);
      }, 3000); // 3秒间隔
    },
    
    async addVideoToLibrary() {
      if (!this.youtubeUrl) {
        alert(this.t('youtube.enterUrl'));
        return;
      }

      if (!this.systemReady) {
        alert(this.t('youtube.systemNotReady'));
        return;
      }
      
      this.isAdding = true;
      
      try {
        const response = await axios.post('/api/youtube/add', {
          url: this.youtubeUrl,
          difficulty: this.difficulty
        });
        
        if (response.data.success) {
          // 不弹框、不清空输入框，仅刷新视频列表
          this.loadMyVideos();
        }
      } catch (error) {
        console.error('添加视频失败:', error);
        alert(error.response?.data?.error || this.t('youtube.addFailed'));
      } finally {
        this.isAdding = false;
      }
    },

    async parseSubtitles(video) {
      if (video.status === 'parsing') {
        alert(this.t('youtube.isParsing'));
        return;
      }

      if (video.status === 'completed') {
        alert(this.t('youtube.status.completed'));
        return;
      }

      if (!confirm(this.t('youtube.parseConfirm', { title: video.title }))) {
        return;
      }
      
      try {
        const response = await axios.post(`/api/youtube/videos/${video.id}/parse-subtitles`);

        if (response.data.success) {
          alert(this.t('youtube.parseStarted'));

          // 开始轮询状态
          this.parsingVideo = {
            id: video.id,
            videoId: video.videoId,
            status: 'parsing'
          };
          this.isParsing = true;
          this.startPolling(video.id);
        }
      } catch (error) {
        console.error('解析字幕失败:', error);
        alert(error.response?.data?.error || this.t('youtube.parseFailed'));
      }
    },
    
    startPolling(videoId) {
      this.progressPercent = 10;
      this.progressText = 'Fetching video info...';
      
      this.pollInterval = setInterval(async () => {
        try {
          const response = await axios.get(`/api/youtube/videos/${videoId}/status`);
          const video = response.data.video;
          
          this.parsingVideo.status = video.status;
          
          if (video.status === 'parsing') {
            // 模拟进度增长
            this.progressPercent = Math.min(this.progressPercent + 5, 90);
            this.progressText = 'Processing subtitles...';
          } else if (video.status === 'completed') {
            this.progressPercent = 100;
            this.progressText = this.t('common.success');

            setTimeout(() => {
              this.clearPolling();
              this.isParsing = false;
              this.parsingVideo = null;
              this.loadMyVideos();
              alert(this.t('youtube.parseSuccess', { count: video.sentenceCount }));
            }, 1000);
          } else if (video.status === 'failed') {
            this.clearPolling();
            this.isParsing = false;
            alert(this.t('youtube.parseFailed') + ': ' + video.errorMessage);
            this.parsingVideo = null;
          }
        } catch (error) {
          console.error('获取解析状态失败:', error);
        }
      }, 2000);
    },
    
    clearPolling() {
      if (this.pollInterval) {
        clearInterval(this.pollInterval);
        this.pollInterval = null;
      }
    },
    
    cancelParsing() {
      this.clearPolling();
      this.isParsing = false;
      this.parsingVideo = null;
    },
    
    async viewVideoDetails(video) {
      if (video.status === 'added') {
        alert(this.t('youtube.notParsed'));
        return;
      }

      if (video.status === 'parsing') {
        alert(this.t('youtube.isParsing'));
        return;
      }

      if (video.status !== 'completed') {
        alert(this.t('youtube.notCompleted'));
        return;
      }
      
      // 跳转到新页面，使用数据库的 id
      this.$router.push({
        path: '/transcribe',
        query: { videoId: video.videoId }
      });
    },
    
    backToList() {
      this.selectedVideo = null;
      this.videoSentences = [];
      this.loadMyVideos();
    },
    
    async togglePractice(sentence) {
      if (this.practicingSentence?.id === sentence.id) {
        // 收起
        this.closePractice();
      } else {
        // 展开新的练习
        this.closePractice(); // 先关闭之前的
        this.practicingSentence = sentence;
        
        // 等待DOM更新后初始化播放器
        await this.$nextTick();
        this.initYoutubePlayer();
        
        // 创建练习任务
        try {
          const response = await axios.post('/api/follow-read/tasks', {
            sentenceId: sentence.id
          });
          this.currentTask = response.data;
        } catch (error) {
          console.error('创建任务失败:', error);
          alert('Failed to create practice task. Please try again.');
          this.currentTask = null;
        }
      }
    },
    
    closePractice() {
      if (this.youtubePlayer) {
        this.youtubePlayer.destroy();
        this.youtubePlayer = null;
        this.youtubePlayerReady = false;
      }
      if (this.playbackTimer) {
        clearTimeout(this.playbackTimer);
      }
      if (this.taskPollInterval) {
        clearInterval(this.taskPollInterval);
      }
      this.practicingSentence = null;
      this.userAudioUrl = null;
      this.taskResult = null;
      this.isRecording = false;
      this.isPlayingStandard = false;
    },
    
    async checkMicrophonePermission() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        this.canRecord = true;
        stream.getTracks().forEach(track => track.stop());
      } catch (error) {
        console.error('麦克风权限获取失败:', error);
        this.canRecord = false;
      }
    },
    
    loadYoutubeAPI() {
      if (window.YT) return;
      
      const tag = document.createElement('script');
      tag.src = 'https://www.youtube.com/iframe_api';
      const firstScriptTag = document.getElementsByTagName('script')[0];
      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
      
      window.onYouTubeIframeAPIReady = () => {
        console.log('YouTube API Ready');
      };
    },
    
    initYoutubePlayer() {
      if (!this.practicingSentence) return;
      
      if (this.youtubePlayer) {
        this.youtubePlayer.destroy();
      }
      
      this.youtubePlayerReady = false;
      const videoId = this.extractYoutubeVideoId(this.selectedVideo.sourceUrl);
      const containerId = `youtube-practice-${this.practicingSentence.id}`;
      
      const initPlayer = () => {
        if (!window.YT || !window.YT.Player) {
          setTimeout(initPlayer, 100);
          return;
        }
        
        try {
          this.youtubePlayer = new window.YT.Player(containerId, {
            height: '200',
            width: '100%',
            videoId: videoId,
            playerVars: {
              'playsinline': 1,
              'rel': 0,
              'modestbranding': 1
            },
            events: {
              'onReady': () => {
                this.youtubePlayerReady = true;
              },
              'onStateChange': (event) => {
                if (event.data === window.YT.PlayerState.PLAYING) {
                  this.isPlayingStandard = true;
                } else if (event.data === window.YT.PlayerState.PAUSED || 
                           event.data === window.YT.PlayerState.ENDED) {
                  this.isPlayingStandard = false;
                }
              }
            }
          });
        } catch (error) {
          console.error('创建YouTube播放器失败:', error);
        }
      };
      
      initPlayer();
    },
    
    playYoutubeSegment() {
      if (!this.youtubePlayer || !this.youtubePlayerReady) {
        alert(this.t('youtube.practice.playerNotReady'));
        return;
      }
      
      const startTime = this.practicingSentence.startTime;
      const endTime = this.practicingSentence.endTime;
      
      if (this.isPlayingStandard) {
        this.youtubePlayer.pauseVideo();
        this.isPlayingStandard = false;
        if (this.playbackTimer) {
          clearTimeout(this.playbackTimer);
        }
      } else {
        this.youtubePlayer.seekTo(startTime, true);
        this.youtubePlayer.playVideo();
        this.isPlayingStandard = true;
        
        const duration = (endTime - startTime) * 1000;
        this.playbackTimer = setTimeout(() => {
          if (this.youtubePlayer) {
            this.youtubePlayer.pauseVideo();
          }
          this.isPlayingStandard = false;
        }, duration);
      }
    },
    
    async startRecording() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        this.mediaRecorder = new MediaRecorder(stream);
        this.audioChunks = [];

        this.mediaRecorder.ondataavailable = (event) => {
          this.audioChunks.push(event.data);
        };

        this.mediaRecorder.onstop = async () => {
          const audioBlob = new Blob(this.audioChunks, { type: 'audio/wav' });
          this.userAudioUrl = URL.createObjectURL(audioBlob);

          await this.uploadRecording(audioBlob);
          stream.getTracks().forEach(track => track.stop());
        };

        this.mediaRecorder.start();
        this.isRecording = true;
      } catch (error) {
        console.error('录音失败:', error);
        alert(this.t('youtube.practice.recordFailed'));
      }
    },
    
    stopRecording() {
      if (this.mediaRecorder && this.isRecording) {
        this.mediaRecorder.stop();
        this.isRecording = false;
      }
    },
    
    async uploadRecording(audioBlob) {
      try {
        if (!this.currentTask || !this.currentTask.id) {
          console.error('上传录音失败: 任务未创建');
          alert(this.t('youtube.practice.taskNotCreated'));
          return;
        }

        const formData = new FormData();
        formData.append('audio', audioBlob, 'recording.wav');

        await axios.post(`/api/follow-read/tasks/${this.currentTask.id}/submit`, formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });

        this.pollTaskResult();
      } catch (error) {
        console.error('上传录音失败:', error);
        alert(this.t('youtube.practice.uploadFailed'));
      }
    },
    
    pollTaskResult() {
      if (!this.currentTask || !this.currentTask.id) {
        console.error('无法轮询结果: 任务未创建');
        return;
      }
      
      this.taskPollInterval = setInterval(async () => {
        try {
          if (!this.currentTask || !this.currentTask.id) {
            clearInterval(this.taskPollInterval);
            return;
          }
          
          const response = await axios.get(`/api/follow-read/tasks/${this.currentTask.id}`);
          if (response.data.task.status === 'completed') {
            this.taskResult = response.data;
            clearInterval(this.taskPollInterval);
          }
        } catch (error) {
          console.error('获取结果失败:', error);
        }
      }, 1000);
    },
    
    reRecord() {
      this.userAudioUrl = null;
      this.taskResult = null;
      this.audioChunks = [];
      if (this.taskPollInterval) {
        clearInterval(this.taskPollInterval);
      }
    },
    
    extractYoutubeVideoId(url) {
      if (!url) return '';
      const match = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([a-zA-Z0-9_-]{11})/);
      return match ? match[1] : '';
    },
    
    getScoreClass(score) {
      if (score >= 85) return 'excellent';
      if (score >= 70) return 'good';
      if (score >= 60) return 'fair';
      return 'poor';
    },
    
    formatDuration(seconds) {
      if (!seconds) return '--:--';
      const mins = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return `${mins}:${secs.toString().padStart(2, '0')}`;
    },
    
    formatTime(seconds) {
      if (!seconds) return '00:00';
      const mins = Math.floor(seconds / 60);
      const secs = Math.floor(seconds % 60);
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    },
    
    getStatusLabel(status) {
      const labels = {
        'added': this.t('youtube.status.added'),
        'parsing': this.t('youtube.status.parsing'),
        'completed': this.t('youtube.status.completed'),
        'failed': this.t('youtube.status.failed')
      };
      return labels[status] || status;
    },

    getDifficultyLabel(difficulty) {
      const labels = {
        'easy': this.t('youtube.difficulty.easy'),
        'medium': this.t('youtube.difficulty.medium'),
        'hard': this.t('youtube.difficulty.hard')
      };
      return labels[difficulty] || difficulty;
    },

    confirmDeleteVideo(video) {
      const title = video.title || 'this video';
      const sentenceInfo = video.sentenceCount ? this.t('youtube.andSentences', { count: video.sentenceCount }) : '';

      if (confirm(this.t('youtube.deleteConfirm', { title, sentenceInfo }))) {
        this.deleteVideo(video.id);
      }
    },

    async deleteVideo(videoId) {
      try {
        await axios.delete(`/api/youtube/videos/${videoId}`);

        // 从列表中移除
        this.myVideos = this.myVideos.filter(v => v.id !== videoId);

        // 如果当前正在查看这个视频的详情，返回列表
        if (this.selectedVideo && this.selectedVideo.id === videoId) {
          this.backToList();
        }

        alert(this.t('youtube.deleteSuccess'));
      } catch (error) {
        console.error('删除视频失败:', error);
        alert(this.t('youtube.deleteFailed') + ': ' + (error.response?.data?.error || ''));
      }
    },
    
    // ========== 下载相关方法 ==========

    showDownloadOptions(video) {
      if (video.status === 'parsing') {
        alert(this.t('youtube.download.isParsing'));
        return;
      }

      if (video.status === 'failed') {
        alert(this.t('youtube.download.isFailed'));
        return;
      }
      this.downloadingVideo = video;
      this.showDownloadDialog = true;
    },
    
    closeDownloadDialog() {
      this.showDownloadDialog = false;
      this.downloadingVideo = null;
      this.selectedQuality = 'best'; // 重置质量选择
    },
    
    async downloadWithQuality(type) {
      try {
        const response = await axios.post('/api/youtube/download/start', {
          videoId: this.downloadingVideo.id,
          downloadType: type,
          quality: this.selectedQuality,
          formatId: null
        });

        if (response.data.success) {
          const qualityText = this.getQualityText(this.selectedQuality);
          alert(this.t('youtube.download.taskCreated', { quality: qualityText }));
          this.closeDownloadDialog();
          this.loadDownloadTasks();
        }
      } catch (error) {
        console.error('创建下载任务失败:', error);
        alert(this.t('youtube.download.downloadFailed') + ': ' + (error.response?.data?.error || ''));
      }
    },

    async quickDownload(type) {
      try {
        const response = await axios.post(`/api/youtube/download/quick/${this.downloadingVideo.id}`, null, {
          params: { type }
        });

        if (response.data.success) {
          alert(this.t('youtube.download.taskCreated', { quality: '' }));
          this.closeDownloadDialog();
          this.loadDownloadTasks();
        }
      } catch (error) {
        console.error('创建下载任务失败:', error);
        alert(this.t('youtube.download.downloadFailed') + ': ' + (error.response?.data?.error || ''));
      }
    },

    getQualityText(quality) {
      const labels = {
        'best': this.t('youtube.download.best'),
        '4k': this.t('youtube.download.4k'),
        '2k': this.t('youtube.download.2k'),
        '1080p': this.t('youtube.download.1080p'),
        '720p': this.t('youtube.download.720p'),
        '480p': this.t('youtube.download.480p')
      };
      return labels[quality] || quality;
    },

    getQualityDescription(quality) {
      const descriptions = {
        'best': this.t('youtube.download.qualityDesc.best'),
        '4k': this.t('youtube.download.qualityDesc.4k'),
        '2k': this.t('youtube.download.qualityDesc.2k'),
        '1080p': this.t('youtube.download.qualityDesc.1080p'),
        '720p': this.t('youtube.download.qualityDesc.720p'),
        '480p': this.t('youtube.download.qualityDesc.480p')
      };
      return descriptions[quality] || this.t('youtube.download.downloadVideo');
    },
    
    async loadDownloadTasks() {
      try {
        const response = await axios.get('/api/youtube/download/tasks');
        this.downloadTasks = response.data.tasks || [];
        this.updateActiveDownloadCount();
      } catch (error) {
        console.error('加载下载任务失败:', error);
      }
    },
    
    connectSSE() {
      // 确定 SSE 端点 URL（与 axios baseURL 保持一致）
      const baseUrl = window.location.origin;
      const sseUrl = `${baseUrl}/api/youtube/download/progress/stream`;

      try {
        this.sseSource = new EventSource(sseUrl);

        this.sseSource.addEventListener('download-progress', (event) => {
          try {
            const data = JSON.parse(event.data);
            this.handleSseProgress(data);
          } catch (e) {
            console.error('SSE 解析失败:', e);
          }
        });

        this.sseSource.onopen = () => {
          this.sseConnected = true;
          // SSE 连接成功后，停止轮询 fallback
          if (this.downloadPollInterval) {
            clearInterval(this.downloadPollInterval);
            this.downloadPollInterval = null;
          }
        };

        this.sseSource.onerror = () => {
          this.sseConnected = false;
          // SSE 断开，降级为轮询
          if (!this.downloadPollInterval) {
            this.startDownloadPolling();
          }
        };
      } catch (e) {
        console.warn('SSE 不可用，使用轮询 fallback');
        this.startDownloadPolling();
      }
    },

    handleSseProgress(data) {
      const idx = this.downloadTasks.findIndex(t => t.id === data.taskId);
      if (idx !== -1) {
        // 就地更新已有任务
        const task = this.downloadTasks[idx];
        task.status = data.status;
        task.progress = data.progress;
        task.progressMessage = data.progressMessage;
        task.downloadSpeed = data.downloadSpeed;
        task.downloadedBytes = data.downloadedBytes;
        task.totalBytes = data.totalBytes;
        if (data.outputFile) task.outputFile = data.outputFile;
        if (data.errorMessage) task.errorMessage = data.errorMessage;
      } else {
        // 新任务，重新加载整个列表
        this.loadDownloadTasks();
      }
      this.updateActiveDownloadCount();
    },

    startDownloadPolling() {
      // 轮询 fallback（SSE 断开时使用）
      this.downloadPollInterval = setInterval(() => {
        this.loadDownloadTasks();
      }, 3000);
    },
    
    updateActiveDownloadCount() {
      this.activeDownloadCount = this.downloadTasks.filter(
        t => t.status === 'DOWNLOADING' || t.status === 'PARSING' || t.status === 'QUEUED'
      ).length;
    },
    
    async downloadFile(taskId) {
      try {
        window.location.href = `/api/youtube/download/file/${taskId}`;
      } catch (error) {
        console.error('下载文件失败:', error);
        alert(this.t('youtube.download.downloadFailed'));
      }
    },

    async deleteDownloadTask(taskId) {
      if (!confirm(this.t('youtube.download.deleteTask'))) {
        return;
      }

      try {
        await axios.delete(`/api/youtube/download/tasks/${taskId}`);
        this.loadDownloadTasks();
        alert(this.t('youtube.download.taskDeleted'));
      } catch (error) {
        console.error('删除下载任务失败:', error);
        alert(this.t('youtube.download.downloadFailed') + ': ' + (error.response?.data?.error || ''));
      }
    },

    getDownloadStatusLabel(status) {
      const labels = {
        'INIT': this.t('youtube.download.status.init'),
        'QUEUED': this.t('youtube.download.status.queued'),
        'PARSING': this.t('youtube.download.status.parsing'),
        'DOWNLOADING': this.t('youtube.download.status.downloading'),
        'MERGING': this.t('youtube.download.status.merging'),
        'SUCCESS': this.t('youtube.download.status.success'),
        'FAILED': this.t('youtube.download.status.failed')
      };
      return labels[status] || status;
    },

    getDownloadTypeLabel(type) {
      const labels = {
        'video': this.t('youtube.download.type.video'),
        'audio': this.t('youtube.download.type.audio'),
        'video_audio': this.t('youtube.download.type.videoAudio')
      };
      return labels[type] || type;
    },
    
    openTranscribeRead(video) {
      this.$router.push({
        path: '/transcribe',
        query: { videoId: video.videoId }
      });
    },

    openStudyWorkspace(video) {
      this.$router.push(`/dashboard/study-workspace/${video.videoId}`);
    },

    async togglePin(video) {
      try {
        const response = await axios.put(`/api/youtube/videos/${video.id}/pin`);
        if (response.data.success) {
          video.pinned = response.data.pinned;
          this.loadMyVideos(this.currentPage);
        }
      } catch (error) {
        console.error('Toggle pin failed:', error);
      }
    },

    getVideoTitle(videoId) {
      const video = this.myVideos.find(v => v.id === videoId);
      return video ? video.title : '未知视频';
    },

    // ========== 笔记悬浮预览 ==========

    onCardHover(video, event) {
      if (this.noteHoverTimer) clearTimeout(this.noteHoverTimer);
      const cardEl = event.currentTarget;
      this.noteHoverTimer = setTimeout(() => {
        this.fetchAndShowNotePreview(video, cardEl);
      }, 400);
    },

    onCardLeave() {
      if (this.noteHoverTimer) clearTimeout(this.noteHoverTimer);
      // 延迟关闭，让用户可以移到预览卡片上
      setTimeout(() => {
        if (!this.notePreview.hovering) {
          this.notePreview.visible = false;
        }
      }, 150);
    },

    onNotePreviewLeave() {
      this.notePreview.hovering = false;
      this.notePreview.visible = false;
    },

    async fetchAndShowNotePreview(video, cardEl) {
      const vid = video.videoId;
      // 用缓存避免重复请求
      if (this.noteCache[vid] !== undefined) {
        this.showNotePreview(this.noteCache[vid], cardEl);
        return;
      }
      try {
        const res = await axios.get(`/api/study-notes/video/${vid}`);
        const notes = res.data.notes || [];
        this.noteCache[vid] = notes;
        this.showNotePreview(notes, cardEl);
      } catch (e) {
        this.noteCache[vid] = [];
      }
    },

    showNotePreview(notes, cardEl) {
      if (!notes.length) return;
      const rect = cardEl.getBoundingClientRect();
      const viewportW = window.innerWidth;
      // 默认显示在卡片右侧，空间不够则显示在左侧
      let left = rect.right + 8;
      if (left + 320 > viewportW) {
        left = rect.left - 328;
      }
      if (left < 8) left = 8;
      const top = Math.max(8, Math.min(rect.top, window.innerHeight - 300));
      this.notePreview.notes = notes;
      this.notePreview.style = {
        position: 'fixed',
        top: top + 'px',
        left: left + 'px',
        maxHeight: (window.innerHeight - top - 16) + 'px'
      };
      this.notePreview.hovering = false;
      this.notePreview.visible = true;
    },

    stripHtml(html) {
      if (!html) return '';
      return html.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    },

    formatNoteDate(dateStr) {
      if (!dateStr) return '';
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    },

    // ========== 字幕搜索方法 ==========

    async performSearch(page = 0) {
      if (!this.searchKeyword || !this.searchKeyword.trim()) {
        return;
      }

      this.isSearching = true;
      this.searchPage = page;

      try {
        const response = await axios.get('/api/subtitle-search/locate', {
          params: {
            q: this.searchKeyword.trim(),
            limit: 20
          }
        });

        this.searchResults = response.data || [];
        this.searchTotal = this.searchResults.length;
        this.searchTotalPages = 1;
        this.hasSearched = true;

      } catch (error) {
        console.error('搜索失败:', error);
        // 如果 ES 未启用，尝试使用基础搜索
        if (error.response?.status === 503 || error.response?.data?.error?.includes('未启用')) {
          this.searchResults = [];
          this.searchTotal = 0;
          this.hasSearched = true;
          alert(this.t('youtube.search.esNotEnabled'));
        } else {
          alert(this.t('youtube.search.searchFailed') + ': ' + (error.response?.data?.error || error.message));
        }
      } finally {
        this.isSearching = false;
      }
    },

    clearSearch() {
      this.searchKeyword = '';
      this.searchResults = [];
      this.searchTotal = 0;
      this.searchPage = 0;
      this.searchTotalPages = 0;
      this.hasSearched = false;
    },

    searchPreviousPage() {
      if (this.searchPage > 0) {
        this.performSearch(this.searchPage - 1);
      }
    },

    searchNextPage() {
      if (this.searchPage < this.searchTotalPages - 1) {
        this.performSearch(this.searchPage + 1);
      }
    },

    goToVideo(videoId) {
      this.$router.push({
        path: '/transcribe',
        query: { videoId: videoId }
      });
    },

    goToVideoAtTime(videoId, startTime) {
      this.$router.push({
        path: '/transcribe',
        query: { videoId: videoId, t: Math.floor(startTime) }
      });
    }
  }
};
</script>

<style>
@import '../../assets/styles/youtube-import.css';
</style>

