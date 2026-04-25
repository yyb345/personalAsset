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
          <div class="header-top">
            <h1 class="transcript-title">Transcript</h1>
            <div class="export-dropdown" ref="exportDropdown">
              <button class="export-btn" @click="toggleExportMenu">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                Export
              </button>
              <div v-if="showExportMenu" class="export-menu">
                <button @click="exportAs('pdf')">PDF</button>
                <button @click="exportAs('word')">Word</button>
                <button @click="exportAs('markdown')">Markdown</button>
              </div>
            </div>
          </div>
          <p class="transcript-subtitle">{{ videoData.videoTitle }}</p>
        </div>

        <div class="transcript-content" ref="transcriptContent" @click="handleTranscriptClick">
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
                v-html="renderHighlightedText(sentence)"
              ></span>
            </p>
          </div>
        </div>

        <!-- Highlight Toolbar -->
        <div
          v-if="showHighlightToolbar"
          class="highlight-toolbar"
          :style="{ top: toolbarPosition.top + 'px', left: toolbarPosition.left + 'px' }"
        >
          <button @click="addHighlight('yellow')" class="highlight-btn yellow" title="黄色高亮">
            <span class="color-dot"></span>
          </button>
          <button @click="addHighlight('green')" class="highlight-btn green" title="绿色高亮">
            <span class="color-dot"></span>
          </button>
          <button @click="addHighlight('blue')" class="highlight-btn blue" title="蓝色高亮">
            <span class="color-dot"></span>
          </button>
          <button @click="addHighlight('pink')" class="highlight-btn pink" title="粉色高亮">
            <span class="color-dot"></span>
          </button>
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
import { jsPDF } from 'jspdf'
import { Document, Packer, Paragraph, TextRun, HeadingLevel } from 'docx'
import { saveAs } from 'file-saver'

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
      timeUpdateInterval: null,
      // Highlight feature
      highlights: [],
      showHighlightToolbar: false,
      toolbarPosition: { top: 0, left: 0 },
      pendingSelection: null,
      // Export feature
      showExportMenu: false
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

    // Load saved highlights
    this.loadHighlights()

    // Listen for text selection
    document.addEventListener('mouseup', this.handleTextSelection)
    document.addEventListener('click', this.handleDocumentClick)
  },
  beforeUnmount() {
    if (this.timeUpdateInterval) {
      clearInterval(this.timeUpdateInterval)
    }
    if (this.player) {
      this.player.destroy()
    }
    document.removeEventListener('mouseup', this.handleTextSelection)
    document.removeEventListener('click', this.handleDocumentClick)
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
    },

    // Highlight methods
    handleTextSelection(e) {
      const selection = window.getSelection()
      const selectedText = selection.toString().trim()

      if (!selectedText || selectedText.length < 2) {
        return
      }

      // Check if selection is within transcript content
      const transcriptContent = this.$refs.transcriptContent
      if (!transcriptContent || !transcriptContent.contains(selection.anchorNode)) {
        return
      }

      // Find which sentence contains the selection
      const sentenceEl = selection.anchorNode.parentElement?.closest('.sentence')
      if (!sentenceEl) return

      const sentenceId = this.findSentenceIdFromElement(sentenceEl)
      if (!sentenceId) return

      // Get position for toolbar (use fixed positioning)
      const range = selection.getRangeAt(0)
      const rect = range.getBoundingClientRect()

      this.toolbarPosition = {
        top: rect.top - 45,
        left: rect.left + rect.width / 2 - 70
      }

      this.pendingSelection = {
        text: selectedText,
        sentenceId: sentenceId
      }

      this.showHighlightToolbar = true
    },

    handleDocumentClick(e) {
      if (!e.target.closest('.highlight-toolbar')) {
        this.showHighlightToolbar = false
        this.pendingSelection = null
      }
      // Close export menu when clicking outside
      if (!e.target.closest('.export-dropdown')) {
        this.showExportMenu = false
      }
    },

    handleTranscriptClick(e) {
      const highlightEl = e.target.closest('.highlight')
      if (highlightEl) {
        const highlightId = parseInt(highlightEl.dataset.id)
        if (highlightId && confirm('删除这个高亮？')) {
          this.removeHighlight(highlightId)
        }
      }
    },

    findSentenceIdFromElement(el) {
      if (!this.videoData?.sentences) return null
      const index = Array.from(this.$refs.transcriptContent.querySelectorAll('.sentence')).indexOf(el)
      if (index >= 0 && this.videoData.sentences[index]) {
        return this.videoData.sentences[index].id
      }
      return null
    },

    addHighlight(color) {
      if (!this.pendingSelection) return

      const highlight = {
        id: Date.now(),
        text: this.pendingSelection.text,
        sentenceId: this.pendingSelection.sentenceId,
        color: color,
        createdAt: new Date().toISOString()
      }

      this.highlights.push(highlight)
      this.saveHighlights()

      this.showHighlightToolbar = false
      this.pendingSelection = null
      window.getSelection().removeAllRanges()
    },

    removeHighlight(highlightId) {
      this.highlights = this.highlights.filter(h => h.id !== highlightId)
      this.saveHighlights()
    },

    loadHighlights() {
      const key = `highlights_${this.videoId}`
      const saved = localStorage.getItem(key)
      if (saved) {
        try {
          this.highlights = JSON.parse(saved)
        } catch (e) {
          this.highlights = []
        }
      }
    },

    saveHighlights() {
      const key = `highlights_${this.videoId}`
      localStorage.setItem(key, JSON.stringify(this.highlights))
    },

    renderHighlightedText(sentence) {
      let text = sentence.text + ' '
      const sentenceHighlights = this.highlights.filter(h => h.sentenceId === sentence.id)

      if (sentenceHighlights.length === 0) {
        return this.escapeHtml(text)
      }

      // Sort by text length (longer first) to handle overlapping
      sentenceHighlights.sort((a, b) => b.text.length - a.text.length)

      // Create a map of positions to highlight
      let result = text
      for (const hl of sentenceHighlights) {
        const escapedText = this.escapeHtml(hl.text)
        const regex = new RegExp(this.escapeRegex(hl.text), 'gi')
        result = result.replace(regex, `<mark class="highlight highlight-${hl.color}" data-id="${hl.id}">${escapedText}</mark>`)
      }

      return result
    },

    escapeHtml(text) {
      const div = document.createElement('div')
      div.textContent = text
      return div.innerHTML
    },

    escapeRegex(string) {
      return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    },

    // Export methods
    toggleExportMenu() {
      this.showExportMenu = !this.showExportMenu
    },

    getPlainTranscript() {
      if (!this.videoData?.sentences) return ''
      return this.videoData.sentences.map(s => s.text).join(' ')
    },

    getTranscriptWithTimestamps() {
      if (!this.paragraphs) return []
      return this.paragraphs.map(p => ({
        time: this.formatTime(p.startTime),
        text: p.sentences.map(s => s.text).join(' ')
      }))
    },

    async exportAs(format) {
      this.showExportMenu = false
      const title = this.videoData?.videoTitle || 'Transcript'
      const safeTitle = title.replace(/[<>:"/\\|?*]/g, '_').substring(0, 100)

      try {
        switch (format) {
          case 'pdf':
            await this.exportPDF(title, safeTitle)
            break
          case 'word':
            await this.exportWord(title, safeTitle)
            break
          case 'markdown':
            this.exportMarkdown(title, safeTitle)
            break
        }
      } catch (err) {
        console.error('Export failed:', err)
        alert('导出失败: ' + err.message)
      }
    },

    async exportPDF(title, filename) {
      const doc = new jsPDF()
      const pageWidth = doc.internal.pageSize.getWidth()
      const margin = 20
      const maxWidth = pageWidth - margin * 2
      let y = 20

      // Title
      doc.setFontSize(18)
      doc.setFont(undefined, 'bold')
      const titleLines = doc.splitTextToSize(title, maxWidth)
      doc.text(titleLines, margin, y)
      y += titleLines.length * 8 + 10

      // Content
      doc.setFontSize(11)
      doc.setFont(undefined, 'normal')

      const paragraphs = this.getTranscriptWithTimestamps()
      for (const p of paragraphs) {
        // Check if need new page
        if (y > 270) {
          doc.addPage()
          y = 20
        }

        // Timestamp
        doc.setTextColor(150)
        doc.text(p.time, margin, y)
        y += 6

        // Text
        doc.setTextColor(0)
        const lines = doc.splitTextToSize(p.text, maxWidth)
        doc.text(lines, margin, y)
        y += lines.length * 5 + 8
      }

      doc.save(`${filename}.pdf`)
    },

    async exportWord(title, filename) {
      const paragraphs = this.getTranscriptWithTimestamps()

      const children = [
        new Paragraph({
          text: title,
          heading: HeadingLevel.HEADING_1,
          spacing: { after: 300 }
        })
      ]

      for (const p of paragraphs) {
        children.push(
          new Paragraph({
            children: [
              new TextRun({ text: p.time, color: '999999', size: 20 })
            ],
            spacing: { before: 200 }
          }),
          new Paragraph({
            children: [
              new TextRun({ text: p.text, size: 24 })
            ],
            spacing: { after: 100 }
          })
        )
      }

      const doc = new Document({
        sections: [{ children }]
      })

      const blob = await Packer.toBlob(doc)
      saveAs(blob, `${filename}.docx`)
    },

    exportMarkdown(title, filename) {
      const paragraphs = this.getTranscriptWithTimestamps()
      let md = `# ${title}\n\n`

      for (const p of paragraphs) {
        md += `**${p.time}**\n\n${p.text}\n\n`
      }

      const blob = new Blob([md], { type: 'text/markdown;charset=utf-8' })
      saveAs(blob, `${filename}.md`)
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

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.transcript-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  color: #1a1a1a;
}

/* Export Dropdown */
.export-dropdown {
  position: relative;
}

.export-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: background 0.2s;
}

.export-btn:hover {
  background: #e5e7eb;
}

.export-btn svg {
  flex-shrink: 0;
}

.export-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 50;
  min-width: 120px;
}

.export-menu button {
  display: block;
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  text-align: left;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: background 0.15s;
}

.export-menu button:hover {
  background: #f3f4f6;
}

.export-menu button:not(:last-child) {
  border-bottom: 1px solid #f3f4f6;
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

/* Highlight Toolbar */
.highlight-toolbar {
  position: fixed;
  display: flex;
  gap: 6px;
  padding: 8px 12px;
  background: #1f2937;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
  animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.highlight-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s;
}

.highlight-btn:hover {
  transform: scale(1.15);
}

.highlight-btn .color-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
}

.highlight-btn.yellow { background: #fef3c7; }
.highlight-btn.yellow .color-dot { background: #fbbf24; }

.highlight-btn.green { background: #d1fae5; }
.highlight-btn.green .color-dot { background: #34d399; }

.highlight-btn.blue { background: #dbeafe; }
.highlight-btn.blue .color-dot { background: #60a5fa; }

.highlight-btn.pink { background: #fce7f3; }
.highlight-btn.pink .color-dot { background: #f472b6; }

/* Highlight Marks (use :deep for v-html content) */
:deep(.highlight) {
  border-radius: 3px;
  padding: 1px 2px;
  margin: 0 -2px;
  cursor: pointer;
  transition: filter 0.2s;
}

:deep(.highlight:hover) {
  filter: brightness(0.95);
}

:deep(.highlight-yellow) { background: #fef08a; }
:deep(.highlight-green) { background: #bbf7d0; }
:deep(.highlight-blue) { background: #bfdbfe; }
:deep(.highlight-pink) { background: #fbcfe8; }

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
