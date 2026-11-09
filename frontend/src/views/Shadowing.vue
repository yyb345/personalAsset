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
      <h2>Load Failed</h2>
      <p>{{ error }}</p>
      <button @click="retry" class="retry-btn">Retry</button>
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
              <span class="sentence-count">📝 {{ videoData.totalSentences }} learning sentences</span>
            </div>
          </div>
        </div>
        <button @click="goBack" class="close-btn" title="Close">✕</button>
      </header>

      <!-- 控制面板 -->
      <div class="control-panel">
        <div class="speed-control">
          <label>Speed:</label>
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
          <label>Difficulty:</label>
          <button 
            @click="difficultyFilter = 'all'"
            :class="{ active: difficultyFilter === 'all' }"
            class="filter-btn"
          >
            All
          </button>
          <button 
            @click="difficultyFilter = 'easy'"
            :class="{ active: difficultyFilter === 'easy' }"
            class="filter-btn difficulty-easy"
          >
            Easy
          </button>
          <button 
            @click="difficultyFilter = 'medium'"
            :class="{ active: difficultyFilter === 'medium' }"
            class="filter-btn difficulty-medium"
          >
            Medium
          </button>
          <button 
            @click="difficultyFilter = 'hard'"
            :class="{ active: difficultyFilter === 'hard' }"
            class="filter-btn difficulty-hard"
          >
            Hard
          </button>
        </div>

        <div class="loop-control">
          <label>
            <input type="checkbox" v-model="loopEnabled" />
            Loop Sentence
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
            class="sentence-item-wrapper"
          >
            <!-- 句子卡片 -->
            <div 
              :class="[
                'sentence-item',
                { 'active': currentSentenceIndex === index },
                { 'playing': isPlaying && currentSentenceIndex === index },
                { 'expanded': expandedSentenceId === sentence.id },
                `difficulty-${sentence.difficulty}`
              ]"
              @click="playSentence(index)"
            >
              <div class="sentence-content">
                <div class="sentence-header">
                  <span class="sentence-number">#{{ index + 1 }}</span>
                  <span :class="['difficulty-badge', `badge-${sentence.difficulty}`]">
                    {{ getDifficultyLabel(sentence.difficulty) }}
                  </span>
                  <span class="sentence-time">{{ formatTime(sentence.startTime) }}</span>
                </div>
              <p class="sentence-text">{{ sentence.text }}</p>
            </div>
              <button 
                class="expand-btn" 
                @click.stop="toggleExpand(sentence)"
                :title="expandedSentenceId === sentence.id ? 'Collapse' : 'Expand video player'"
              >
                {{ expandedSentenceId === sentence.id ? '▼' : '▶' }}
              </button>
            </div>

            <!-- 展开的视频播放器区域 -->
            <transition name="slide-down">
              <div v-if="expandedSentenceId === sentence.id" class="expanded-video-panel">
                <div class="video-player-container">
                  <h3>Video Player</h3>
                  <div :id="'youtube-player-' + sentence.id" class="youtube-player-container"></div>
                  <div class="player-controls">
                    <button 
                      class="play-segment-btn" 
                      @click="playYoutubeSegment(sentence)"
                      :disabled="!sentencePlayers[sentence.id]?.ready"
                    >
                      {{ sentencePlayers[sentence.id]?.playing ? '⏸ Pause' : '▶ Play Segment' }}
                    </button>
                    <span class="time-hint">
                      {{ formatTime(sentence.startTime) }} - {{ formatTime(sentence.endTime) }}
                    </span>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </div>

        <!-- 无句子提示 -->
        <div v-if="filteredSentences.length === 0" class="no-sentences">
          <p>😕 No sentences match the filter</p>
        </div>
      </div>

      <!-- 底部播放控制器 -->
      <div v-if="currentSentence" class="player-footer">
        <div class="current-sentence-display">
          <div class="sentence-info">
            <span class="current-index">Sentence {{ currentSentenceIndex + 1 }} / {{ filteredSentences.length }}</span>
            <span class="current-time">{{ formatTime(currentSentence.startTime) }} - {{ formatTime(currentSentence.endTime) }}</span>
          </div>
          <p class="current-text">{{ currentSentence.text }}</p>
        </div>
        <div class="player-controls">
          <button @click="previousSentence" class="control-btn" :disabled="currentSentenceIndex === 0">
            ⏮️ Previous
          </button>
          <button @click="togglePlay" class="control-btn primary">
            {{ isPlaying ? '⏸️ Pause' : '▶️ Play' }}
          </button>
          <button @click="stopPlay" class="control-btn" :disabled="!isPlaying">
            ⏹️ Stop
          </button>
          <button @click="nextSentence" class="control-btn" :disabled="currentSentenceIndex === filteredSentences.length - 1">
            ⏭️ Next
          </button>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import { trackPlayer, consumeLearningDetails } from '@/utils/learningAnalytics'
import axios from '@/utils/axios'

export default {
  name: 'Shadowing',
  data() {
    return {
      loading: true,
      loadingMessage: 'Loading subtitles...',
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
      sentenceRefs: [],
      
      // 展开控制
      expandedSentenceId: null,
      sentencePlayers: {} // 存储每个句子的播放器实例
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
    // 从路由参数获取 videoId（优先使用 Vue Router 的 query，兼容直接 URL 参数）
    this.videoId = this.$route.query.videoId
    
    // 如果没有从 query 获取到，尝试从 URL 中提取（兼容旧方式）
    if (!this.videoId) {
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
    }
    
    if (!this.videoId) {
      this.error = 'Missing video ID parameter'
      this.loading = false
      return
    }
    
    // 加载 YouTube IFrame API
    await this.loadYouTubeAPI()
    
    // 加载视频数据
    await this.loadVideoData()
    
    if (this.analyticsDisposed) return
    // Count actual sentence playback across this material visit without collecting IDs.
    let activePlayer
    this.stopLearningAnalytics = trackPlayer({
      getCurrentTime: () => {
        activePlayer = [this.player, ...Object.values(this.sentencePlayers).map(item => item.player)]
          .find(player => player && player.getPlayerState() === 1)
        return activePlayer ? activePlayer.getCurrentTime() : 0
      },
      getPlayerState: () => activePlayer ? 1 : 2,
      getPlaybackRate: () => activePlayer?.getPlaybackRate?.() || 1
    }, consumeLearningDetails())
    // 绑定键盘事件
    window.addEventListener('keydown', this.handleKeyboard)
  },
  beforeUnmount() {
    this.analyticsDisposed = true
    this.stopLearningAnalytics?.()
    // 清理
    window.removeEventListener('keydown', this.handleKeyboard)
    if (this.loopInterval) {
      clearInterval(this.loopInterval)
    }
    if (this.player) {
      this.player.destroy()
    }
    // 清理所有句子播放器
    Object.values(this.sentencePlayers).forEach(playerData => {
      if (playerData && playerData.player) {
        playerData.player.destroy()
      }
    })
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
        this.loadingMessage = 'Fetching video info...'
        
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
          throw new Error(statusRes.data.message || 'Subtitle parsing failed')
        } else {
          // 触发解析
          await axios.post('/api/youtube/parse', {
            videoId: this.videoId
          })
          await this.pollParseStatus()
        }
        
      } catch (err) {
        console.error('加载视频数据失败:', err)
        this.error = err.response?.data?.error || err.message || 'Load failed'
        this.loading = false
      }
    },
    
    async pollParseStatus() {
      this.loadingMessage = 'Parsing subtitles, please wait...'
      
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
              reject(new Error(res.data.message || 'Parsing failed'))
            } else {
              this.loadingMessage = res.data.message || 'Parsing...'
            }
          } catch (err) {
            clearInterval(interval)
            reject(err)
          }
        }, 2000) // 每2秒查询一次
        
        // 超时保护（5分钟）
        setTimeout(() => {
          clearInterval(interval)
          reject(new Error('Parsing timeout'))
        }, 5 * 60 * 1000)
      })
    },
    
    async initPlayer() {
      // 不再需要全局播放器，改为按需创建
    },
    
    toggleExpand(sentence) {
      if (this.expandedSentenceId === sentence.id) {
        // 收起
        this.expandedSentenceId = null
        // 清理播放器
        if (this.sentencePlayers[sentence.id]) {
          const player = this.sentencePlayers[sentence.id].player
          if (player) {
            player.destroy()
          }
          delete this.sentencePlayers[sentence.id]
        }
      } else {
        // 收起之前的
        if (this.expandedSentenceId) {
          const oldPlayer = this.sentencePlayers[this.expandedSentenceId]
          if (oldPlayer && oldPlayer.player) {
            oldPlayer.player.destroy()
          }
          delete this.sentencePlayers[this.expandedSentenceId]
        }
        
        // 展开新的
        this.expandedSentenceId = sentence.id
        
        // 等待 DOM 更新后初始化播放器
        this.$nextTick(() => {
          this.initSentencePlayer(sentence)
        })
      }
    },
    
    async initSentencePlayer(sentence) {
      try {
        if (!window.YT || !window.YT.Player) {
          console.error('YouTube API 未加载')
          return
        }
        
        const containerId = `youtube-player-${sentence.id}`
        
        // 检查容器是否存在
        const container = document.getElementById(containerId)
        if (!container) {
          console.error('播放器容器不存在:', containerId)
          return
        }
        
        const player = new window.YT.Player(containerId, {
          videoId: this.videoId,
          height: '200',
          width: '100%',
          playerVars: {
            autoplay: 0,
            controls: 1,
            disablekb: 0,
            modestbranding: 1,
            rel: 0,
            playsinline: 1
          },
          events: {
            onReady: () => {
              // 定位到句子的开始时间
              player.seekTo(sentence.startTime, true)
              
              // 保存播放器实例
              if (!this.sentencePlayers[sentence.id]) {
                this.$set(this.sentencePlayers, sentence.id, {
                  player: player,
                  ready: true,
                  playing: false
                })
              } else {
                this.sentencePlayers[sentence.id].player = player
                this.sentencePlayers[sentence.id].ready = true
              }
            },
            onStateChange: (event) => {
              if (this.sentencePlayers[sentence.id]) {
                if (event.data === window.YT.PlayerState.PLAYING) {
                  this.sentencePlayers[sentence.id].playing = true
                } else if (event.data === window.YT.PlayerState.PAUSED || 
                           event.data === window.YT.PlayerState.ENDED) {
                  this.sentencePlayers[sentence.id].playing = false
                }
              }
            }
          }
        })
      } catch (error) {
        console.error('初始化句子播放器失败:', error)
      }
    },
    
    playYoutubeSegment(sentence) {
      if (!sentence || !this.sentencePlayers[sentence.id]) return
      
      const playerData = this.sentencePlayers[sentence.id]
      if (!playerData.player || !playerData.ready) return
      
      const player = playerData.player
      
      if (playerData.playing) {
        // 暂停
        player.pauseVideo()
      } else {
        // 播放片段
        player.seekTo(sentence.startTime, true)
        player.setPlaybackRate(this.playbackSpeed)
        player.playVideo()
        
        // 设置定时器在句子结束时暂停
        const duration = (sentence.endTime - sentence.startTime) * 1000
        setTimeout(() => {
          if (playerData.player && playerData.playing) {
            playerData.player.pauseVideo()
          }
        }, duration)
      }
    },
    
    playSentence(index) {
      this.currentSentenceIndex = index
      const sentence = this.filteredSentences[index]
      
      if (!sentence) return
      
      // 如果有展开的播放器，使用它播放
      if (this.expandedSentenceId === sentence.id && this.sentencePlayers[sentence.id]) {
        this.playYoutubeSegment(sentence)
        this.isPlaying = true
      } else {
        // 否则使用隐藏的播放器（如果存在）
        if (this.player) {
          this.isPlaying = true
          this.player.setPlaybackRate(this.playbackSpeed)
          this.player.seekTo(sentence.startTime, true)
          this.player.playVideo()
          
          // 设置定时器在句子结束时停止
          const duration = (sentence.endTime - sentence.startTime) * 1000
          setTimeout(() => {
            this.onSentenceEnd()
          }, duration)
        }
      }
      
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
        const sentence = this.currentSentence
        if (sentence && this.expandedSentenceId === sentence.id && this.sentencePlayers[sentence.id]) {
          const player = this.sentencePlayers[sentence.id].player
          if (player) {
            player.pauseVideo()
          }
        } else if (this.player) {
          this.player.pauseVideo()
        }
      }
    },
    
    togglePlay() {
      const sentence = this.currentSentence
      if (!sentence) return
      
      if (this.isPlaying) {
        this.isPlaying = false
        // 暂停当前播放器
        if (this.expandedSentenceId === sentence.id && this.sentencePlayers[sentence.id]) {
          const player = this.sentencePlayers[sentence.id].player
          if (player) {
            player.pauseVideo()
          }
        } else if (this.player) {
          this.player.pauseVideo()
        }
      } else {
        this.playSentence(this.currentSentenceIndex)
      }
    },
    
    stopPlay() {
      this.isPlaying = false
      const sentence = this.currentSentence
      if (sentence && this.expandedSentenceId === sentence.id && this.sentencePlayers[sentence.id]) {
        const player = this.sentencePlayers[sentence.id].player
        if (player) {
          player.pauseVideo()
        }
      } else if (this.player) {
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
        easy: 'Easy',
        medium: 'Medium',
        hard: 'Hard'
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
      // 尝试返回上一页，如果没有历史记录则关闭窗口
      if (window.history.length > 1) {
        this.$router.go(-1)
      } else {
        this.$router.push('/dashboard/youtube')
      }
    }
  }
}
</script>

<style scoped>
.shadowing-container {
  min-height: 100vh;
  background: #FFFFFF;
  color: #0F1419;
  font-family: 'Roboto', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
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
  background: #FFFFFF;
}

.loading-spinner {
  width: 60px;
  height: 60px;
  border: 4px solid #E5E5E5;
  border-top-color: #FF0000;
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
  padding: 10px 16px;
  background: #0F0F0F;
  color: #FFFFFF;
  border: none;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-btn:hover {
  background: #272727;
}

/* 主界面 */
.shadowing-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #FFFFFF;
}

/* 视频头部 */
.video-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #FFFFFF;
  border: 1px solid #E5E5E5;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.video-info {
  display: flex;
  gap: 16px;
  flex: 1;
}

.thumbnail {
  width: 160px;
  height: 90px;
  border-radius: 8px;
  object-fit: cover;
  background: #F2F2F2;
}

.info-text h1 {
  font-size: 18px;
  font-weight: 500;
  margin: 0 0 8px 0;
  color: #0F0F0F;
  line-height: 1.4;
}

.meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606060;
}

.meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.close-btn {
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #0F0F0F;
  font-size: 24px;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: #F2F2F2;
}

/* 句子项包装器 */
.sentence-item-wrapper {
  margin-bottom: 12px;
}

.sentence-item {
  position: relative;
}

.expand-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  background: #FFFFFF;
  border: 1px solid #DADADA;
  border-radius: 6px;
  color: #0F0F0F;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 10;
}

.expand-btn:hover {
  background: #F9F9F9;
  border-color: #0F0F0F;
}

.sentence-item.expanded {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

/* 展开的视频面板 */
.expanded-video-panel {
  background: #FAFAFA;
  border: 1px solid #E5E5E5;
  border-top: none;
  border-radius: 0 0 12px 12px;
  padding: 16px;
  margin-top: -12px;
}

.video-player-container {
  width: 100%;
}

.video-player-container h3 {
  font-size: 14px;
  font-weight: 500;
  color: #0F0F0F;
  margin: 0 0 12px 0;
}

.youtube-player-container {
  width: 100%;
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  background: #000000;
}

.player-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.play-segment-btn {
  padding: 8px 16px;
  background: #0F0F0F;
  color: #FFFFFF;
  border: none;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.play-segment-btn:hover:not(:disabled) {
  background: #272727;
}

.play-segment-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.time-hint {
  font-size: 13px;
  color: #606060;
}

/* 动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
  max-height: 500px;
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}

/* 控制面板 */
.control-panel {
  display: flex;
  gap: 24px;
  align-items: center;
  flex-wrap: wrap;
  background: #FFFFFF;
  border: 1px solid #E5E5E5;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.control-panel label {
  font-size: 14px;
  color: #0F0F0F;
  margin-right: 8px;
  font-weight: 400;
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
  padding: 8px 12px;
  background: #FFFFFF;
  border: 1px solid #DADADA;
  border-radius: 18px;
  color: #0F0F0F;
  font-size: 14px;
  font-weight: 400;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 36px;
}

.speed-btn:hover,
.filter-btn:hover {
  background: #F9F9F9;
}

.speed-btn.active,
.filter-btn.active {
  background: #0F0F0F;
  color: #FFFFFF;
  border-color: #0F0F0F;
}

.loop-control label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #0F0F0F;
}

.loop-control input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #0F0F0F;
}

/* 句子列表 */
.sentences-container {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 16px;
  min-height: 400px;
  margin-bottom: 120px;
}

.sentence-item {
  background: #FFFFFF;
  border: 1px solid #E5E5E5;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.sentence-item:hover {
  background: #F9F9F9;
  border-color: #DADADA;
}

.sentence-item.active {
  border-color: #0F0F0F;
  background: #FAFAFA;
}

.sentence-item.playing {
  border-color: #0F0F0F;
  background: #F5F5F5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.sentence-content {
  display: block;
}

.sentence-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #606060;
}

.sentence-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 24px;
  background: #0F0F0F;
  color: #FFFFFF;
  border-radius: 4px;
  font-weight: 500;
  font-size: 12px;
}

.difficulty-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.badge-easy {
  background: #E8F5E9;
  color: #2E7D32;
}

.badge-medium {
  background: #FFF3E0;
  color: #E65100;
}

.badge-hard {
  background: #FFEBEE;
  color: #C62828;
}

.sentence-text {
  font-size: 14px;
  line-height: 1.6;
  color: #0F0F0F;
  margin: 0 0 6px 0;
}

.sentence-controls {
  display: flex;
  flex-direction: row;
  gap: 12px;
  align-items: center;
  margin-top: 4px;
}

.play-btn,
.loop-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s, opacity 0.2s;
  color: #909090;
  opacity: 0.6;
}

.play-btn:hover,
.loop-btn:hover {
  color: #0F0F0F;
  opacity: 1;
}

.play-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.play-btn:disabled:hover {
  color: #909090;
  opacity: 0.3;
}

.loop-btn.active {
  color: #0F0F0F;
  opacity: 1;
}

.loop-btn.active:hover {
  color: #0F0F0F;
  opacity: 1;
}

.btn-icon {
  font-size: 17px;
  line-height: 1;
  display: inline-block;
}

.no-sentences {
  text-align: center;
  padding: 60px 20px;
  color: #606060;
}

/* 底部播放器 */
.player-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #FFFFFF;
  border-top: 1px solid #E5E5E5;
  padding: 16px 20px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.current-sentence-display {
  margin-bottom: 12px;
  color: #0F0F0F;
}

.sentence-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #606060;
  margin-bottom: 6px;
}

.current-text {
  font-size: 14px;
  font-weight: 400;
  margin: 0;
  color: #0F0F0F;
  line-height: 1.5;
}

.player-controls {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.control-btn {
  padding: 10px 16px;
  background: #FFFFFF;
  color: #0F0F0F;
  border: 1px solid #DADADA;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 36px;
}

.control-btn:hover:not(:disabled) {
  background: #F9F9F9;
}

.control-btn.primary {
  background: #0F0F0F;
  color: #FFFFFF;
  border-color: #0F0F0F;
}

.control-btn.primary:hover:not(:disabled) {
  background: #272727;
  border-color: #272727;
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #F9F9F9;
  color: #909090;
  border-color: #E5E5E5;
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
  
  .shadowing-main {
    padding: 12px;
  }
}
</style>

