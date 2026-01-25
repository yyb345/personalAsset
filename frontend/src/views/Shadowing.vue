<template>
  <div class="shadowing-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-screen">
      <div class="loading-spinner"></div>
      <p>{{ loadingMessage }}</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-screen">
      <div class="error-icon">⚠️</div>
      <h2>加载失败</h2>
      <p>{{ error }}</p>
      <button @click="retry" class="retry-btn">重试</button>
    </div>

    <!-- 主界面 -->
    <div v-else-if="videoData" class="shadowing-main">
      <!-- 顶部信息栏 -->
      <header class="video-header">
        <div class="video-info">
          <img v-if="videoData.thumbnailUrl" :src="videoData.thumbnailUrl" alt="thumbnail" class="thumbnail" />
          <div class="info-text">
            <h1>{{ videoData.videoTitle }}</h1>
            <div class="meta">
              <span class="channel">📺 {{ videoData.channel }}</span>
              <span class="duration">⏱️ {{ formatDuration(videoData.duration) }}</span>
              <span class="sentence-count">📝 {{ videoData.totalSentences }} 个学习句子</span>
            </div>
          </div>
        </div>
        <button @click="goBack" class="close-btn" title="关闭">✕</button>
      </header>

      <!-- 控制面板 -->
      <div class="control-panel">
        <div class="speed-control">
          <label>速度：</label>
          <button 
            v-for="speed in speedOptions" 
            :key="speed"
            @click="playbackSpeed = speed"
            :class="{ active: playbackSpeed === speed }"
            class="speed-btn"
          >
            {{ speed }}x
          </button>
        </div>
        
        <div class="filter-control">
          <label>难度筛选：</label>
          <button 
            @click="difficultyFilter = 'all'"
            :class="{ active: difficultyFilter === 'all' }"
            class="filter-btn"
          >
            全部
          </button>
          <button 
            @click="difficultyFilter = 'easy'"
            :class="{ active: difficultyFilter === 'easy' }"
            class="filter-btn difficulty-easy"
          >
            简单
          </button>
          <button 
            @click="difficultyFilter = 'medium'"
            :class="{ active: difficultyFilter === 'medium' }"
            class="filter-btn difficulty-medium"
          >
            中等
          </button>
          <button 
            @click="difficultyFilter = 'hard'"
            :class="{ active: difficultyFilter === 'hard' }"
            class="filter-btn difficulty-hard"
          >
            困难
          </button>
        </div>

        <div class="loop-control">
          <label>
            <input type="checkbox" v-model="loopEnabled" />
            单句循环
          </label>
        </div>
      </div>

      <!-- 句子列表 -->
      <div class="sentences-container">
        <div class="sentences-list">
          <div 
            v-for="(sentence, index) in filteredSentences" 
            :key="sentence.id"
            :ref="el => setSentenceRef(index, el)"
            @click="playSentence(index)"
            :class="[
              'sentence-item',
              { 'active': currentSentenceIndex === index },
              { 'playing': isPlaying && currentSentenceIndex === index },
              `difficulty-${sentence.difficulty}`
            ]"
          >
            <div class="sentence-header">
              <span class="sentence-number">#{{ index + 1 }}</span>
              <span :class="['difficulty-badge', `badge-${sentence.difficulty}`]">
                {{ getDifficultyLabel(sentence.difficulty) }}
              </span>
              <span class="sentence-time">{{ formatTime(sentence.startTime) }}</span>
            </div>
            <p class="sentence-text">{{ sentence.text }}</p>
            <div class="sentence-controls">
              <button 
                @click.stop="playSentence(index)" 
                class="play-btn"
                :disabled="isPlaying && currentSentenceIndex === index"
              >
                {{ isPlaying && currentSentenceIndex === index ? '▶️ 播放中' : '▶️ 播放' }}
              </button>
              <button 
                @click.stop="toggleLoop(index)" 
                class="loop-btn"
                :class="{ active: loopEnabled && currentSentenceIndex === index }"
              >
                🔁 循环
              </button>
            </div>
          </div>
        </div>

        <!-- 无句子提示 -->
        <div v-if="filteredSentences.length === 0" class="no-sentences">
          <p>😕 没有符合筛选条件的句子</p>
        </div>
      </div>

      <!-- 底部播放控制器 -->
      <div v-if="currentSentence" class="player-footer">
        <div class="current-sentence-display">
          <div class="sentence-info">
            <span class="current-index">句子 {{ currentSentenceIndex + 1 }} / {{ filteredSentences.length }}</span>
            <span class="current-time">{{ formatTime(currentSentence.startTime) }} - {{ formatTime(currentSentence.endTime) }}</span>
          </div>
          <p class="current-text">{{ currentSentence.text }}</p>
        </div>
        <div class="player-controls">
          <button @click="previousSentence" class="control-btn" :disabled="currentSentenceIndex === 0">
            ⏮️ 上一句
          </button>
          <button @click="togglePlay" class="control-btn primary">
            {{ isPlaying ? '⏸️ 暂停' : '▶️ 播放' }}
          </button>
          <button @click="stopPlay" class="control-btn" :disabled="!isPlaying">
            ⏹️ 停止
          </button>
          <button @click="nextSentence" class="control-btn" :disabled="currentSentenceIndex === filteredSentences.length - 1">
            ⏭️ 下一句
          </button>
        </div>
      </div>

      <!-- 隐藏的 YouTube 播放器 -->
      <div id="youtube-player" style="display: none;"></div>
    </div>

    <!-- 快捷键提示 -->
    <div class="keyboard-hints" v-if="!loading && !error">
      <p>⌨️ 快捷键: <kbd>Space</kbd> 播放/暂停 | <kbd>←</kbd> 上一句 | <kbd>→</kbd> 下一句 | <kbd>L</kbd> 循环</p>
    </div>
  </div>
</template>

<script>
import axios from '@/utils/axios'

export default {
  name: 'Shadowing',
  data() {
    return {
      loading: true,
      loadingMessage: '正在加载字幕...',
      error: null,
      videoId: null,
      videoData: null,
      
      // 播放控制
      player: null,
      isPlaying: false,
      currentSentenceIndex: 0,
      playbackSpeed: 1.0,
      loopEnabled: false,
      speedOptions: [0.5, 0.75, 1.0, 1.25, 1.5],
      
      // 筛选
      difficultyFilter: 'all',
      
      // 循环控制
      loopInterval: null,
      sentenceRefs: []
    }
  },
  computed: {
    filteredSentences() {
      if (!this.videoData || !this.videoData.sentences || !Array.isArray(this.videoData.sentences)) {
        return []
      }
      if (this.difficultyFilter === 'all') {
        return this.videoData.sentences
      }
      return this.videoData.sentences.filter(s => s.difficulty === this.difficultyFilter)
    },
    currentSentence() {
      if (!this.filteredSentences || this.filteredSentences.length === 0) {
        return null
      }
      return this.filteredSentences[this.currentSentenceIndex] || null
    }
  },
  async mounted() {
    // 从 URL 获取 videoId（支持 Hash 路由）
    let urlParams
    if (window.location.hash.includes('?')) {
      // Hash 模式：从 hash 中提取参数
      const hashQuery = window.location.hash.split('?')[1]
      urlParams = new URLSearchParams(hashQuery)
    } else {
      // History 模式：从 search 中提取参数
      urlParams = new URLSearchParams(window.location.search)
    }
    
    this.videoId = urlParams.get('videoId')
    
    if (!this.videoId) {
      this.error = '缺少视频 ID 参数'
      this.loading = false
      return
    }
    
    // 加载 YouTube IFrame API
    await this.loadYouTubeAPI()
    
    // 加载视频数据
    await this.loadVideoData()
    
    // 绑定键盘事件
    window.addEventListener('keydown', this.handleKeyboard)
  },
  beforeUnmount() {
    // 清理
    window.removeEventListener('keydown', this.handleKeyboard)
    if (this.loopInterval) {
      clearInterval(this.loopInterval)
    }
    if (this.player) {
      this.player.destroy()
    }
  },
  methods: {
    async loadYouTubeAPI() {
      // 如果已经加载，直接返回
      if (window.YT && window.YT.Player) {
        return Promise.resolve()
      }
      
      return new Promise((resolve) => {
        // 创建脚本标签
        const tag = document.createElement('script')
        tag.src = 'https://www.youtube.com/iframe_api'
        
        // 设置回调
        window.onYouTubeIframeAPIReady = () => {
          resolve()
        }
        
        const firstScriptTag = document.getElementsByTagName('script')[0]
        if (firstScriptTag && firstScriptTag.parentNode) {
          firstScriptTag.parentNode.insertBefore(tag, firstScriptTag)
        } else {
          document.head.appendChild(tag)
        }
      })
    },
    
    async loadVideoData() {
      try {
        this.loadingMessage = '正在获取视频信息...'
        
        // 先检查状态
        const statusRes = await axios.get(`/api/youtube/status/${this.videoId}`)
        
        if (statusRes.data.status === 'parsing') {
          // 正在解析，轮询等待
          await this.pollParseStatus()
        } else if (statusRes.data.status === 'completed') {
          // 已完成，获取数据
          const res = await axios.get(`/api/youtube/sentences/${this.videoId}`)
          this.videoData = res.data
          this.loading = false
          
          // 初始化 YouTube 播放器
          await this.initPlayer()
        } else if (statusRes.data.status === 'failed') {
          throw new Error(statusRes.data.message || '字幕解析失败')
        } else {
          // 触发解析
          await axios.post('/api/youtube/parse', {
            videoId: this.videoId
          })
          await this.pollParseStatus()
        }
        
      } catch (err) {
        console.error('加载视频数据失败:', err)
        this.error = err.response?.data?.error || err.message || '加载失败'
        this.loading = false
      }
    },
    
    async pollParseStatus() {
      this.loadingMessage = '正在解析字幕，请稍候...'
      
      return new Promise((resolve, reject) => {
        const interval = setInterval(async () => {
          try {
            const res = await axios.get(`/api/youtube/status/${this.videoId}`)
            
            if (res.data.status === 'completed') {
              clearInterval(interval)
              const dataRes = await axios.get(`/api/youtube/sentences/${this.videoId}`)
              this.videoData = dataRes.data
              this.loading = false
              await this.initPlayer()
              resolve()
            } else if (res.data.status === 'failed') {
              clearInterval(interval)
              reject(new Error(res.data.message || '解析失败'))
            } else {
              this.loadingMessage = res.data.message || '解析中...'
            }
          } catch (err) {
            clearInterval(interval)
            reject(err)
          }
        }, 2000) // 每2秒查询一次
        
        // 超时保护（5分钟）
        setTimeout(() => {
          clearInterval(interval)
          reject(new Error('解析超时'))
        }, 5 * 60 * 1000)
      })
    },
    
    async initPlayer() {
      try {
        if (!window.YT || !window.YT.Player) {
          console.error('YouTube API 未加载')
          return
        }
        
        this.player = new window.YT.Player('youtube-player', {
          videoId: this.videoId,
          playerVars: {
            autoplay: 0,
            controls: 0,
            disablekb: 1,
            modestbranding: 1
          },
          events: {
            onReady: () => {
              console.log('YouTube 播放器已准备')
            },
            onStateChange: (event) => {
              if (event.data === window.YT.PlayerState.ENDED) {
                this.onSentenceEnd()
              }
            }
          }
        })
      } catch (error) {
        console.error('初始化播放器失败:', error)
      }
    },
    
    playSentence(index) {
      if (!this.player) return
      
      this.currentSentenceIndex = index
      const sentence = this.filteredSentences[index]
      
      if (!sentence) return
      
      this.isPlaying = true
      this.player.setPlaybackRate(this.playbackSpeed)
      this.player.seekTo(sentence.startTime, true)
      this.player.playVideo()
      
      // 设置定时器在句子结束时停止
      const duration = (sentence.endTime - sentence.startTime) * 1000
      setTimeout(() => {
        this.onSentenceEnd()
      }, duration)
      
      // 滚动到当前句子
      this.$nextTick(() => {
        const el = this.sentenceRefs[index]
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      })
    },
    
    onSentenceEnd() {
      if (this.loopEnabled) {
        // 循环播放当前句子
        setTimeout(() => {
          this.playSentence(this.currentSentenceIndex)
        }, 300)
      } else {
        this.isPlaying = false
        this.player.pauseVideo()
      }
    },
    
    togglePlay() {
      if (this.isPlaying) {
        this.isPlaying = false
        this.player.pauseVideo()
      } else {
        this.playSentence(this.currentSentenceIndex)
      }
    },
    
    stopPlay() {
      this.isPlaying = false
      if (this.player) {
        this.player.pauseVideo()
      }
    },
    
    nextSentence() {
      if (this.currentSentenceIndex < this.filteredSentences.length - 1) {
        this.playSentence(this.currentSentenceIndex + 1)
      }
    },
    
    previousSentence() {
      if (this.currentSentenceIndex > 0) {
        this.playSentence(this.currentSentenceIndex - 1)
      }
    },
    
    toggleLoop(index) {
      if (this.currentSentenceIndex === index) {
        this.loopEnabled = !this.loopEnabled
      } else {
        this.currentSentenceIndex = index
        this.loopEnabled = true
      }
    },
    
    handleKeyboard(e) {
      // 忽略输入框中的按键
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
        return
      }
      
      switch (e.code) {
        case 'Space':
          e.preventDefault()
          this.togglePlay()
          break
        case 'ArrowLeft':
          e.preventDefault()
          this.previousSentence()
          break
        case 'ArrowRight':
          e.preventDefault()
          this.nextSentence()
          break
        case 'KeyL':
          e.preventDefault()
          this.loopEnabled = !this.loopEnabled
          break
      }
    },
    
    setSentenceRef(index, el) {
      if (el) {
        if (!this.sentenceRefs) {
          this.sentenceRefs = []
        }
        this.sentenceRefs[index] = el
      }
    },
    
    getDifficultyLabel(difficulty) {
      const labels = {
        easy: '简单',
        medium: '中等',
        hard: '困难'
      }
      return labels[difficulty] || difficulty
    },
    
    formatTime(seconds) {
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins}:${secs.toString().padStart(2, '0')}`
    },
    
    formatDuration(seconds) {
      const hours = Math.floor(seconds / 3600)
      const mins = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      
      if (hours > 0) {
        return `${hours}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
      }
      return `${mins}:${secs.toString().padStart(2, '0')}`
    },
    
    retry() {
      this.loading = true
      this.error = null
      this.loadVideoData()
    },
    
    goBack() {
      window.close()
    }
  }
}
</script>

<style scoped>
.shadowing-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
}

/* 加载和错误状态 */
.loading-screen,
.error-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}

.loading-spinner {
  width: 60px;
  height: 60px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.retry-btn {
  margin-top: 20px;
  padding: 12px 24px;
  background: white;
  color: #667eea;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s;
}

.retry-btn:hover {
  transform: translateY(-2px);
}

/* 主界面 */
.shadowing-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 视频头部 */
.video-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
}

.video-info {
  display: flex;
  gap: 20px;
  flex: 1;
}

.thumbnail {
  width: 160px;
  height: 90px;
  border-radius: 8px;
  object-fit: cover;
}

.info-text h1 {
  font-size: 24px;
  margin: 0 0 10px 0;
}

.meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  opacity: 0.9;
}

.close-btn {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: white;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

/* 控制面板 */
.control-panel {
  display: flex;
  gap: 30px;
  align-items: center;
  flex-wrap: wrap;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.control-panel label {
  font-size: 14px;
  margin-right: 8px;
}

.speed-control,
.filter-control,
.loop-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.speed-btn,
.filter-btn {
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.speed-btn:hover,
.filter-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.speed-btn.active,
.filter-btn.active {
  background: white;
  color: #667eea;
  border-color: white;
}

/* 句子列表 */
.sentences-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  min-height: 400px;
  margin-bottom: 120px;
}

.sentence-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.sentence-item:hover {
  background: #e9ecef;
  transform: translateX(4px);
}

.sentence-item.active {
  border-color: #667eea;
  background: #f0f3ff;
}

.sentence-item.playing {
  border-color: #764ba2;
  background: linear-gradient(135deg, #f0f3ff 0%, #fdf0ff 100%);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(102, 126, 234, 0); }
}

.sentence-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #6c757d;
}

.sentence-number {
  font-weight: 600;
  color: #667eea;
}

.difficulty-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.badge-easy {
  background: #d4edda;
  color: #155724;
}

.badge-medium {
  background: #fff3cd;
  color: #856404;
}

.badge-hard {
  background: #f8d7da;
  color: #721c24;
}

.sentence-text {
  font-size: 16px;
  line-height: 1.6;
  color: #212529;
  margin: 0 0 12px 0;
}

.sentence-controls {
  display: flex;
  gap: 8px;
}

.play-btn,
.loop-btn {
  padding: 6px 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.play-btn:hover,
.loop-btn:hover {
  background: #5568d3;
  transform: translateY(-2px);
}

.play-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.loop-btn.active {
  background: #764ba2;
}

.no-sentences {
  text-align: center;
  padding: 60px 20px;
  color: #6c757d;
}

/* 底部播放器 */
.player-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(102, 126, 234, 0.2);
  padding: 16px 20px;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
}

.current-sentence-display {
  margin-bottom: 12px;
  color: #212529;
}

.sentence-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #6c757d;
  margin-bottom: 6px;
}

.current-text {
  font-size: 16px;
  font-weight: 500;
  margin: 0;
  color: #212529;
}

.player-controls {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.control-btn {
  padding: 10px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.control-btn:hover:not(:disabled) {
  background: #5568d3;
  transform: translateY(-2px);
}

.control-btn.primary {
  background: #764ba2;
}

.control-btn.primary:hover:not(:disabled) {
  background: #653a8c;
}

.control-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

/* 快捷键提示 */
.keyboard-hints {
  position: fixed;
  bottom: 140px;
  right: 20px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 12px;
}

kbd {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  margin: 0 2px;
}

/* 响应式 */
@media (max-width: 768px) {
  .video-info {
    flex-direction: column;
  }
  
  .thumbnail {
    width: 100%;
    height: auto;
  }
  
  .control-panel {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .keyboard-hints {
    display: none;
  }
}
</style>

