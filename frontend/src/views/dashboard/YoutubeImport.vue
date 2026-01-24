<template>
  <div class="youtube-import-container">
    <div class="header">
      <h1>AI-powered YouTube Shadowing</h1>
      <p class="subtitle">Turn any YouTube video into intelligent English shadowing practice</p>
    </div>

    <!-- YouTube URL 输入区 -->
    <div v-if="!selectedVideo" class="import-section">
      <div class="url-input-card">
        <p class="hint">Paste a YouTube link. We'll generate shadowing practice instantly.</p>
        
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
          <span v-if="!isAdding">Generate Practice</span>
          <span v-else class="ai-processing">
            <span class="processing-text">
              <span class="stage">Analyzing video</span>
              <span class="separator">·</span>
              <span class="stage">Extracting subtitles</span>
              <span class="separator">·</span>
              <span class="stage">Generating practice</span>
            </span>
          </span>
        </button>

        <!-- 系统状态检查 -->
        <div v-if="!systemReady" class="warning-box">
          <p>⚠️ 系统未就绪：yt-dlp 未安装</p>
          <p class="hint-text">请在服务器上安装 yt-dlp: <code>pip install yt-dlp</code></p>
        </div>
      </div>

      <!-- 我的视频列表 -->
      <div class="my-videos-section">
        <h2>My Library</h2>
        <div v-if="myVideos.length === 0" class="empty-state">
          <p>No videos yet</p>
        </div>
        <div v-else class="video-list">
          <div 
            v-for="video in myVideos" 
            :key="video.id"
            class="video-card"
          >
            <div class="video-thumbnail" @click="viewVideoDetails(video)">
              <img v-if="video.thumbnailUrl" :src="video.thumbnailUrl" alt="thumbnail" />
              <div v-else class="thumbnail-placeholder">📹</div>
            </div>
            <div class="video-info" @click="viewVideoDetails(video)">
              <h3 class="video-title">{{ video.title || 'Loading...' }}</h3>
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
                    class="action-btn download-btn" 
                    @click.stop="showDownloadOptions(video)"
                    title="Download Video"
                    :disabled="video.status !== 'completed' && video.status !== 'added'">
                    <Download :size="16" :stroke-width="2" />
                    <span class="kbd">D</span>
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
      </div>
    </div>

    <!-- 解析进度展示 -->
    <div v-if="parsingVideo" class="parsing-progress">
      <div class="progress-card">
        <h2>Processing video...</h2>
        <div class="progress-info">
          <p><strong>Video ID:</strong> {{ parsingVideo.videoId }}</p>
          <p><strong>Status:</strong> {{ getStatusLabel(parsingVideo.status) }}</p>
        </div>
        
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <p class="progress-text">{{ progressText }}</p>

        <button class="cancel-btn" @click="cancelParsing">Cancel</button>
      </div>
    </div>

    <!-- 视频详情和句子列表 -->
    <div v-if="selectedVideo" class="video-details">
      <button class="back-btn" @click="backToList">← Back</button>
      
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
        <h2>Practice Sentences</h2>
        <div v-if="!videoSentences || videoSentences.length === 0" class="empty-state">
          <p>No sentences available</p>
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
                {{ practicingSentence?.id === sentence.id ? 'Collapse' : 'Practice' }}
              </button>
            </div>

            <!-- 折叠的练习区域 -->
            <transition name="slide-down">
              <div v-if="practicingSentence?.id === sentence.id" class="practice-panel">
                <!-- YouTube播放器 -->
                <div class="practice-player">
                  <h3>Reference Audio</h3>
                  <div :id="'youtube-practice-' + sentence.id" class="youtube-practice-container"></div>
                  <button 
                    class="play-segment-btn" 
                    @click="playYoutubeSegment"
                    :disabled="!youtubePlayerReady"
                  >
                    {{ isPlayingStandard ? 'Stop' : 'Play' }}
                  </button>
                  <p class="time-hint">{{ formatTime(sentence.startTime) }} - {{ formatTime(sentence.endTime) }}</p>
                </div>

                <!-- 录音区域 -->
                <div class="practice-recording">
                  <h3>Your Recording</h3>
                  
                  <!-- 未录音状态 -->
                  <div v-if="!isRecording && !userAudioUrl" class="record-ready">
                    <p>Ready to practice?</p>
                    <button class="record-btn" @click="startRecording" :disabled="!canRecord">
                      Start Recording
                    </button>
                  </div>

                  <!-- 录音中 -->
                  <div v-if="isRecording" class="recording-active">
                    <div class="recording-indicator">
                      <span class="pulse"></span>
                      Recording...
                    </div>
                    <button class="stop-record-btn" @click="stopRecording">
                      Stop
                    </button>
                  </div>

                  <!-- 已录音 -->
                  <div v-if="userAudioUrl" class="recorded">
                    <audio :src="userAudioUrl" controls></audio>
                    <button class="rerecord-btn" @click="reRecord">
                      Re-record
                    </button>
                  </div>

                  <!-- 评分结果 -->
                  <div v-if="taskResult && taskResult.task.status === 'completed'" class="score-result">
                    <div class="overall-score">
                      <div class="score-circle" :class="getScoreClass(taskResult.task.overallScore)">
                        <span class="score-value">{{ taskResult.task.overallScore }}</span>
                        <span class="score-label">Score</span>
                      </div>
                    </div>
                    
                    <div class="score-details">
                      <div class="score-bar-item">
                        <span>Pronunciation</span>
                        <div class="bar">
                          <div class="fill" :style="{ width: taskResult.task.pronunciationScore + '%' }"></div>
                        </div>
                        <span>{{ taskResult.task.pronunciationScore }}</span>
                      </div>
                      <div class="score-bar-item">
                        <span>Fluency</span>
                        <div class="bar">
                          <div class="fill fluency" :style="{ width: taskResult.task.fluencyScore + '%' }"></div>
                        </div>
                        <span>{{ taskResult.task.fluencyScore }}</span>
                      </div>
                      <div class="score-bar-item">
                        <span>Intonation</span>
                        <div class="bar">
                          <div class="fill intonation" :style="{ width: taskResult.task.intonationScore + '%' }"></div>
                        </div>
                        <span>{{ taskResult.task.intonationScore }}</span>
                      </div>
                    </div>
                    
                    <!-- 单词级别详细反馈 -->
                    <div class="word-feedback-section">
                      <h3>Word-level Feedback</h3>
                      
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
                          <span class="dot correct"></span> Correct
                        </span>
                        <span class="legend-item">
                          <span class="dot incorrect"></span> Need improvement
                        </span>
                      </div>
                      
                      <!-- 错误单词详细提示 -->
                      <div v-if="incorrectWords.length > 0" class="error-tips">
                        <h4>Suggestions</h4>
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
                        <p>🎉 Perfect! All words are pronounced correctly!</p>
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
        <h2>Download Video</h2>
          <button class="close-btn" @click="closeDownloadDialog">×</button>
        </div>
        
        <div class="dialog-content">
          <h3>{{ downloadingVideo?.title }}</h3>
          
          <div class="quality-selector">
            <label>Video Quality</label>
            <select v-model="selectedQuality" class="quality-select">
              <option value="best">Best (Auto)</option>
              <option value="4k">4K (2160p)</option>
              <option value="2k">2K (1440p)</option>
              <option value="1080p">Full HD (1080p)</option>
              <option value="720p">HD (720p)</option>
              <option value="480p">SD (480p)</option>
            </select>
            <p class="quality-hint">Tip: Choose 1080p for learning, best quality for archiving</p>
          </div>
          
          <div class="download-options">
            <div class="option-card" @click="downloadWithQuality('video')">
              <div class="option-icon">🎬</div>
              <div class="option-info">
                <h4>Download Video</h4>
                <p>{{ getQualityDescription(selectedQuality) }}</p>
              </div>
            </div>
            
            <div class="option-card" @click="quickDownload('audio')">
              <div class="option-icon">🎵</div>
              <div class="option-info">
                <h4>Audio Only (MP3)</h4>
                <p>Extract and convert to MP3 format</p>
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
          <h2>My Downloads</h2>
          <button class="close-btn" @click="showDownloadTasks = false">×</button>
        </div>
        
        <div class="dialog-content">
          <div v-if="downloadTasks.length === 0" class="empty-state">
            <p>No download tasks</p>
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
                <div v-if="task.status === 'DOWNLOADING'" class="progress-bar">
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
  </div>
</template>

<script>
import axios from '../../utils/axios';
import { Pencil, Download, Trash2 } from 'lucide-vue-next';

export default {
  name: 'YoutubeImport',
  components: {
    Pencil,
    Download,
    Trash2
  },
  data() {
    return {
      youtubeUrl: '',
      difficulty: 'auto', // 固定使用 auto，让 AI 自动判断
      systemReady: true,
      isAdding: false,
      isParsing: false,
      parsingVideo: null,
      progressPercent: 0,
      progressText: 'Preparing...',
      
      myVideos: [],
      selectedVideo: null,
      videoSentences: [],
      
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
      selectedQuality: 'best'
    };
  },
  computed: {
    incorrectWords() {
      if (!this.taskResult || !this.taskResult.results) return [];
      return this.taskResult.results.filter(r => r.status === 'incorrect');
    }
  },
  mounted() {
    this.checkSystemStatus();
    this.loadMyVideos();
    this.checkMicrophonePermission();
    this.loadYoutubeAPI();
    this.loadDownloadTasks();
    this.startDownloadPolling();
    this.startVideoListPolling(); // 新增：启动视频列表轮询
  },
  beforeUnmount() {
    this.clearPolling();
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
    if (this.videoListPollInterval) { // 新增：清理视频列表轮询
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
    
    async loadMyVideos() {
      try {
        const response = await axios.get('/api/youtube/videos');
        this.myVideos = response.data;
      } catch (error) {
        console.error('加载视频列表失败:', error);
      }
    },
    
    startVideoListPolling() {
      // 每3秒轮询一次视频列表，以更新状态
      this.videoListPollInterval = setInterval(() => {
        this.loadMyVideos();
      }, 3000); // 3秒间隔
    },
    
    async addVideoToLibrary() {
      if (!this.youtubeUrl) {
        alert('Please enter a YouTube video link');
        return;
      }
      
      if (!this.systemReady) {
        alert('System not ready. Please install yt-dlp first');
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
        alert(error.response?.data?.error || 'Failed to add video. Please check the URL');
      } finally {
        this.isAdding = false;
      }
    },
    
    async parseSubtitles(video) {
      if (video.status === 'parsing') {
        alert('Subtitles are being parsed. Please wait...');
        return;
      }
      
      if (video.status === 'completed') {
        alert('Subtitles already parsed');
        return;
      }
      
      if (!confirm(`Parse subtitles for "${video.title}"?\nThis may take a few minutes.`)) {
        return;
      }
      
      try {
        const response = await axios.post(`/api/youtube/videos/${video.id}/parse-subtitles`);
        
        if (response.data.success) {
          alert('✅ Subtitle parsing started!\nYou can check the progress in the list');
          
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
        alert(error.response?.data?.error || 'Failed to parse. Please try again');
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
            this.progressText = 'Completed!';
            
            setTimeout(() => {
              this.clearPolling();
              this.isParsing = false;
              this.parsingVideo = null;
              this.loadMyVideos();
              alert(`Success! Generated ${video.sentenceCount} practice sentences`);
            }, 1000);
          } else if (video.status === 'failed') {
            this.clearPolling();
            this.isParsing = false;
            alert('Parsing failed: ' + video.errorMessage);
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
        alert('Subtitles not parsed yet\nClick the 📝 button to parse and generate sentences');
        return;
      }
      
      if (video.status === 'parsing') {
        alert('Subtitles are being parsed. Please check back later');
        return;
      }
      
      if (video.status !== 'completed') {
        alert('Video parsing incomplete. Please check back later');
        return;
      }
      
      try {
        const response = await axios.get(`/api/youtube/videos/${video.id}`);
        this.selectedVideo = response.data.video;
        this.videoSentences = response.data.sentences || [];
      } catch (error) {
        console.error('获取视频详情失败:', error);
        alert('Failed to load. Please try again');
      }
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
        alert('Player not ready. Please wait...');
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
        alert('Recording failed. Please check microphone permissions');
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
        alert('Upload failed. Please try again');
      }
    },
    
    pollTaskResult() {
      this.taskPollInterval = setInterval(async () => {
        try {
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
        'added': 'Added',
        'parsing': 'Processing',
        'completed': 'Ready',
        'failed': 'Failed'
      };
      return labels[status] || status;
    },
    
    getDifficultyLabel(difficulty) {
      const labels = {
        'easy': 'Easy',
        'medium': 'Medium',
        'hard': 'Hard'
      };
      return labels[difficulty] || difficulty;
    },
    
    confirmDeleteVideo(video) {
      const title = video.title || 'this video';
      const sentenceInfo = video.sentenceCount ? `and its ${video.sentenceCount} practice sentences` : '';
      
      if (confirm(`Delete "${title}" ${sentenceInfo}?\n\nThis action cannot be undone!`)) {
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
        
        alert('Video deleted successfully');
      } catch (error) {
        console.error('删除视频失败:', error);
        alert('Delete failed: ' + (error.response?.data?.error || 'Please try again'));
      }
    },
    
    // ========== 下载相关方法 ==========
    
    showDownloadOptions(video) {
      if (video.status === 'parsing') {
        alert('Video is being parsed. Please wait until completion');
        return;
      }
      
      if (video.status === 'failed') {
        alert('Video parsing failed. Cannot download');
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
          alert(`✅ Download task created!\nQuality: ${qualityText}\nCheck progress in the bottom right corner`);
          this.closeDownloadDialog();
          this.loadDownloadTasks();
        }
      } catch (error) {
        console.error('创建下载任务失败:', error);
        alert('Download failed: ' + (error.response?.data?.error || 'Please try again'));
      }
    },
    
    async quickDownload(type) {
      try {
        const response = await axios.post(`/api/youtube/download/quick/${this.downloadingVideo.id}`, null, {
          params: { type }
        });
        
        if (response.data.success) {
          alert('Download task created! Check progress in the bottom right corner');
          this.closeDownloadDialog();
          this.loadDownloadTasks();
        }
      } catch (error) {
        console.error('创建下载任务失败:', error);
        alert('Download failed: ' + (error.response?.data?.error || 'Please try again'));
      }
    },
    
    getQualityText(quality) {
      const labels = {
        'best': 'Best (Auto)',
        '4k': '4K (2160p)',
        '2k': '2K (1440p)',
        '1080p': 'Full HD (1080p)',
        '720p': 'HD (720p)',
        '480p': 'SD (480p)'
      };
      return labels[quality] || quality;
    },
    
    getQualityDescription(quality) {
      const descriptions = {
        'best': 'Auto-select highest available quality',
        '4k': 'Ultra HD video, large file size',
        '2k': '2K high definition video',
        '1080p': 'Full HD video, recommended',
        '720p': 'HD video, moderate file size',
        '480p': 'Standard definition, smaller file'
      };
      return descriptions[quality] || 'Download video file';
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
    
    startDownloadPolling() {
      // 每3秒轮询下载任务状态
      this.downloadPollInterval = setInterval(() => {
        this.loadDownloadTasks();
      }, 3000);
    },
    
    updateActiveDownloadCount() {
      this.activeDownloadCount = this.downloadTasks.filter(
        t => t.status === 'DOWNLOADING' || t.status === 'PARSING'
      ).length;
    },
    
    async downloadFile(taskId) {
      try {
        window.location.href = `/api/youtube/download/file/${taskId}`;
      } catch (error) {
        console.error('下载文件失败:', error);
        alert('Download failed. Please try again');
      }
    },
    
    async deleteDownloadTask(taskId) {
      if (!confirm('Delete this download task? The downloaded file will also be deleted.')) {
        return;
      }
      
      try {
        await axios.delete(`/api/youtube/download/tasks/${taskId}`);
        this.loadDownloadTasks();
        alert('Download task deleted');
      } catch (error) {
        console.error('删除下载任务失败:', error);
        alert('Delete failed: ' + (error.response?.data?.error || 'Please try again'));
      }
    },
    
    getDownloadStatusLabel(status) {
      const labels = {
        'INIT': 'Waiting',
        'PARSING': 'Parsing',
        'DOWNLOADING': 'Downloading',
        'MERGING': 'Merging',
        'SUCCESS': 'Completed',
        'FAILED': 'Failed'
      };
      return labels[status] || status;
    },
    
    getDownloadTypeLabel(type) {
      const labels = {
        'video': 'Video',
        'audio': 'Audio',
        'video_audio': 'Video+Audio'
      };
      return labels[type] || type;
    },
    
    getVideoTitle(videoId) {
      const video = this.myVideos.find(v => v.id === videoId);
      return video ? video.title : '未知视频';
    }
  }
};
</script>

<style>
@import '../../assets/styles/youtube-import.css';
</style>

