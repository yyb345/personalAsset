<template>
  <div class="transcribe-read">
    <!-- Loading -->
    <div v-if="loading" class="loading-screen">
      <div class="loading-spinner"></div>
      <p>{{ loadingMessage }}</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-screen">
      <div class="error-icon">⚠️</div>
      <h2>Load Failed</h2>
      <p>{{ error }}</p>
      <button @click="retry" class="retry-btn">Retry</button>
      <button @click="goBack" class="back-link">← Back to Library</button>
    </div>

    <!-- Main Layout -->
    <div v-else-if="videoData" class="main-layout">
      <!-- Left: Video Player -->
      <div class="left-panel">
        <div class="player-wrapper">
          <div id="yt-player" class="yt-player-container"></div>
        </div>
        <div class="video-info">
          <h2 class="video-title">{{ videoData.videoTitle }}</h2>
          <p class="video-channel">{{ videoData.channel }}</p>
          <div class="video-meta">
            <span>{{ formatDuration(videoData.duration) }}</span>
            <span>{{ videoData.totalSentences }} sentences</span>
          </div>
        </div>
        <button @click="goBack" class="back-btn">← Back to Library</button>
      </div>

      <!-- Right: Transcript Reading Area -->
      <div class="right-panel">
        <div class="transcript-header">
          <h1 class="transcript-title">Transcript</h1>
          <p class="transcript-subtitle">{{ videoData.videoTitle }}</p>
        </div>

        <div class="transcript-content" ref="transcriptContent">
          <div
            v-for="(paragraph, pIndex) in paragraphs"
            :key="pIndex"
            :class="['paragraph', { 'active': isParagraphActive(paragraph) }]"
            @click="seekToParagraph(paragraph)"
          >
            <span class="paragraph-time">{{ formatTime(paragraph.startTime) }}</span>
            <p class="paragraph-text">
              <span
                v-for="sentence in paragraph.sentences"
                :key="sentence.id"
                :class="['sentence', { 'current': currentSentenceId === sentence.id }]"
                @click.stop="seekToSentence(sentence)"
              >{{ sentence.text }} </span>
            </p>
          </div>
        </div>

        <!-- Bottom floating stats -->
        <div class="reading-stats">
          <span>{{ totalWords }} words</span>
          <span class="dot">·</span>
          <span>~{{ readingTime }} min read</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from '../utils/axios'

export default {
  name: 'TranscribeRead',
  data() {
    return {
      videoId: null,
      videoData: null,
      loading: true,
      loadingMessage: 'Loading transcript...',
      error: null,
      player: null,
      playerReady: false,
      currentTime: 0,
      currentSentenceId: null,
      timeUpdateInterval: null
    }
  },
  computed: {
    paragraphs() {
      if (!this.videoData || !this.videoData.sentences) return []
      const sentences = this.videoData.sentences
      const groups = []
      for (let i = 0; i < sentences.length; i += 4) {
        const chunk = sentences.slice(i, i + 4)
        groups.push({
          startTime: chunk[0].startTime,
          endTime: chunk[chunk.length - 1].endTime,
          sentences: chunk
        })
      }
      return groups
    },
    totalWords() {
      if (!this.videoData || !this.videoData.sentences) return 0
      return this.videoData.sentences.reduce((sum, s) => {
        return sum + s.text.split(/\s+/).filter(w => w.length > 0).length
      }, 0)
    },
    readingTime() {
      return Math.max(1, Math.ceil(this.totalWords / 200))
    }
  },
  async mounted() {
    this.videoId = this.$route.query.videoId || new URLSearchParams(window.location.search).get('videoId')

    if (!this.videoId) {
      this.error = 'Missing video ID parameter'
      this.loading = false
      return
    }

    await this.loadYouTubeAPI()
    await this.loadVideoData()
  },
  beforeUnmount() {
    if (this.timeUpdateInterval) {
      clearInterval(this.timeUpdateInterval)
    }
    if (this.player) {
      this.player.destroy()
    }
  },
  methods: {
    async loadVideoData() {
      try {
        this.loadingMessage = 'Checking video status...'
        const statusRes = await axios.get(`/api/youtube/status/${this.videoId}`)

        if (statusRes.data.status === 'completed') {
          this.loadingMessage = 'Loading transcript...'
          const res = await axios.get(`/api/youtube/sentences/${this.videoId}`)
          this.videoData = res.data
          this.loading = false
          await this.$nextTick()
          this.initPlayer()
        } else if (statusRes.data.status === 'parsing') {
          this.error = 'Video is still being parsed. Please try again later.'
          this.loading = false
        } else if (statusRes.data.status === 'failed') {
          this.error = statusRes.data.message || 'Subtitle parsing failed'
          this.loading = false
        } else {
          this.error = 'Subtitles not parsed yet. Please parse them first from the library.'
          this.loading = false
        }
      } catch (err) {
        console.error('Failed to load video data:', err)
        this.error = err.response?.data?.error || err.message || 'Load failed'
        this.loading = false
      }
    },

    loadYouTubeAPI() {
      return new Promise((resolve) => {
        if (window.YT && window.YT.Player) {
          resolve()
          return
        }
        const tag = document.createElement('script')
        tag.src = 'https://www.youtube.com/iframe_api'
        const first = document.getElementsByTagName('script')[0]
        first.parentNode.insertBefore(tag, first)

        const prevCallback = window.onYouTubeIframeAPIReady
        window.onYouTubeIframeAPIReady = () => {
          if (prevCallback) prevCallback()
          resolve()
        }
      })
    },

    initPlayer() {
      if (!window.YT || !window.YT.Player) return

      this.player = new window.YT.Player('yt-player', {
        videoId: this.videoId,
        playerVars: {
          playsinline: 1,
          rel: 0,
          modestbranding: 1
        },
        events: {
          onReady: () => {
            this.playerReady = true
            this.startTimeTracking()
          }
        }
      })
    },

    startTimeTracking() {
      this.timeUpdateInterval = setInterval(() => {
        if (!this.player || !this.playerReady) return
        const state = this.player.getPlayerState()
        if (state === window.YT.PlayerState.PLAYING) {
          this.currentTime = this.player.getCurrentTime()
          this.updateCurrentSentence()
        }
      }, 300)
    },

    updateCurrentSentence() {
      if (!this.videoData || !this.videoData.sentences) return
      const t = this.currentTime
      const found = this.videoData.sentences.find(s => t >= s.startTime && t <= s.endTime)
      this.currentSentenceId = found ? found.id : this.currentSentenceId

      // Auto-scroll to current sentence
      if (found) {
        this.$nextTick(() => {
          const el = this.$refs.transcriptContent?.querySelector('.sentence.current')
          if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' })
          }
        })
      }
    },

    isParagraphActive(paragraph) {
      return this.currentTime >= paragraph.startTime && this.currentTime <= paragraph.endTime
    },

    seekToParagraph(paragraph) {
      if (this.player && this.playerReady) {
        this.player.seekTo(paragraph.startTime, true)
        this.player.playVideo()
      }
    },

    seekToSentence(sentence) {
      if (this.player && this.playerReady) {
        this.player.seekTo(sentence.startTime, true)
        this.player.playVideo()
      }
    },

    formatDuration(seconds) {
      if (!seconds) return '--:--'
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins}:${secs.toString().padStart(2, '0')}`
    },

    formatTime(seconds) {
      if (seconds == null) return '00:00'
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    },

    retry() {
      this.loading = true
      this.error = null
      this.loadVideoData()
    },

    goBack() {
      this.$router.push('/dashboard/youtube')
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.transcribe-read {
  min-height: 100vh;
  background: #ffffff;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #1a1a1a;
}

/* Loading & Error */
.loading-screen,
.error-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top-color: #137fec;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-icon {
  font-size: 48px;
}

.retry-btn {
  padding: 10px 24px;
  background: #137fec;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.retry-btn:hover {
  background: #0f6fd0;
}

.back-link {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  font-size: 14px;
  margin-top: 8px;
}

.back-link:hover {
  color: #137fec;
}

/* Main Layout */
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* Left Panel */
.left-panel {
  width: 40%;
  min-width: 380px;
  background: #f9fafb;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow-y: auto;
}

.player-wrapper {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.yt-player-container {
  width: 100%;
  height: 100%;
}

.yt-player-container iframe {
  width: 100%;
  height: 100%;
}

.video-info {
  margin-top: 20px;
  flex-shrink: 0;
}

.video-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  margin: 0 0 8px 0;
  color: #1a1a1a;
}

.video-channel {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 8px 0;
}

.video-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #9ca3af;
}

.back-btn {
  margin-top: auto;
  padding: 10px 0;
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  font-size: 14px;
  text-align: left;
  flex-shrink: 0;
}

.back-btn:hover {
  color: #137fec;
}

/* Right Panel */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.transcript-header {
  padding: 32px 48px 16px;
  border-bottom: 1px solid #f3f4f6;
  flex-shrink: 0;
}

.transcript-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 4px 0;
  color: #1a1a1a;
}

.transcript-subtitle {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Transcript Content */
.transcript-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px 48px 80px;
}

.paragraph {
  display: flex;
  gap: 16px;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.paragraph:hover {
  background: #f3f4f6;
}

.paragraph.active {
  background: #eff6ff;
}

.paragraph-time {
  flex-shrink: 0;
  width: 48px;
  font-size: 12px;
  color: #9ca3af;
  padding-top: 4px;
  font-variant-numeric: tabular-nums;
}

.paragraph-text {
  margin: 0;
  font-size: 17px;
  line-height: 1.8;
  color: #374151;
}

.sentence {
  transition: background 0.2s, color 0.2s;
  border-radius: 4px;
  padding: 1px 0;
}

.sentence:hover {
  color: #137fec;
}

.sentence.current {
  background: #dbeafe;
  color: #1d4ed8;
  padding: 1px 4px;
  margin: 0 -4px;
}

/* Reading Stats */
.reading-stats {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 48px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-top: 1px solid #f3f4f6;
  font-size: 13px;
  color: #9ca3af;
  flex-shrink: 0;
}

.reading-stats .dot {
  color: #d1d5db;
}

/* Responsive */
@media (max-width: 768px) {
  .main-layout {
    flex-direction: column;
    height: auto;
  }

  .left-panel {
    width: 100%;
    min-width: unset;
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
    max-height: 50vh;
  }

  .right-panel {
    min-height: 50vh;
  }

  .transcript-header {
    padding: 24px 24px 12px;
  }

  .transcript-content {
    padding: 16px 24px 80px;
  }

  .reading-stats {
    padding: 12px 24px;
  }
}
</style>
