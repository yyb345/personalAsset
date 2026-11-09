<template>
  <div class="transcribe-read">
    <!-- Loading -->
    <div v-if="loading" class="loading-screen">
      <div class="loading-spinner"></div>
      <p>{{ $te(loadingMessage) ? $t(loadingMessage) : loadingMessage }}</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-screen">
      <div class="error-icon">⚠️</div>
      <h2>{{ $t('transcribe.loadFailed') }}</h2>
      <p>{{ $te(error) ? $t(error) : error }}</p>
      <button @click="retry" class="retry-btn">{{ $t('transcribe.retry') }}</button>
      <button @click="goBack" class="back-link">{{ $t('transcribe.back') }}</button>
    </div>

    <!-- Main Layout -->
    <div v-else-if="videoData" :class="['main-layout', { 'focus-reading': focusReading }]">
      <!-- Left: Video Player -->
      <div v-show="!focusReading" class="left-panel">
        <div class="player-wrapper">
          <div id="yt-player" class="yt-player-container"></div>
        </div>
        <p v-if="playerError" role="status">{{ $te(playerError) ? $t(playerError) : playerError }} <button class="export-btn" @click="preparePlayer">{{ $t('transcribe.retryPlayer') }}</button></p>
        <div class="video-info">
          <h2 class="video-title">{{ videoData.videoTitle }}</h2>
          <p class="video-channel">{{ videoData.channel }}</p>
          <div class="video-meta">
            <span>{{ formatDuration(videoData.duration) }}</span>
            <span>{{ $t('transcribe.sentences', { count: videoData.totalSentences }) }}</span>
          </div>
        </div>
        <section class="keyword-panel" aria-labelledby="keyword-heading" :aria-busy="glossaryLoading">
          <div class="keyword-heading">
            <h2 id="keyword-heading">{{ $t('transcribe.keywords') }}</h2>
            <span>{{ $t('transcribe.terms', { count: glossaryKeywords.length }) }}</span>
          </div>
          <p class="keyword-hint">{{ $t('transcribe.hint') }}</p>
          <ul v-if="glossaryKeywords.length" class="keyword-list">
            <li v-for="term in glossaryKeywords" :key="term.termId || term.termEn">
              <GlossaryText :text="term.termEn" :spans="[{ ...term, start: 0, end: term.termEn.length }]" />
            </li>
          </ul>
          <p v-if="glossaryLoading" class="keyword-status" role="status">{{ $t('transcribe.recognizing') }}</p>
          <p v-else-if="glossaryError" class="keyword-status" role="status">{{ $t('transcribe.incomplete') }} <button class="glossary-retry" @click="loadGlossary">{{ $t('transcribe.retry') }}</button></p>
          <p v-else-if="!glossaryKeywords.length" class="keyword-status">{{ $t('transcribe.empty') }}</p>
        </section>
        <button @click="goBack" class="back-btn">{{ $t('transcribe.back') }}</button>
      </div>

      <!-- Right: Transcript Reading Area -->
      <div class="right-panel">
        <div class="transcript-header">
          <div class="header-top">
            <h1 class="transcript-title">{{ $t('transcribe.transcript') }}</h1>
            <div class="header-actions">
              <select class="export-btn" :aria-label="$t('transcribe.language')" :value="$i18n.locale" @change="changeLocale($event.target.value)">
                <option value="en-US">English</option>
                <option value="zh-CN">中文</option>
              </select>
              <button class="export-btn" :disabled="summaryLoading" :aria-busy="summaryLoading" @click="generateSummary">
                {{ $t(summaryLoading ? 'transcribe.generating' : 'transcribe.summary') }}
              </button>
              <div class="export-dropdown" ref="exportDropdown">
                <button class="export-btn" @click="toggleExportMenu">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  {{ $t('transcribe.export') }}
                </button>
                <div v-if="showExportMenu" class="export-menu">
                  <button @click="exportAs('pdf')">PDF</button>
                  <button @click="exportAs('word')">Word</button>
                  <button @click="exportAs('markdown')">Markdown</button>
                </div>
              </div>
            </div>
          </div>
          <p class="transcript-subtitle">{{ videoData.videoTitle }}</p>
          <div class="reader-controls">
            <div class="font-controls">
              <button class="reader-button" :disabled="fontSize <= 12" :aria-label="$t('transcribe.smallerText')" @click="fontSize--">A−</button>
              <span>{{ fontSize }}</span>
              <button class="reader-button" :disabled="fontSize >= 24" :aria-label="$t('transcribe.largerText')" @click="fontSize++">A+</button>
            </div>
            <button class="reader-button" :aria-pressed="followPlayback" @click="toggleFollow">{{ $t('transcribe.follow') }}</button>
            <button class="reader-button" :aria-pressed="focusReading" @click="focusReading = !focusReading">{{ $t(focusReading ? 'transcribe.exitFocus' : 'transcribe.focus') }}</button>
            <label class="reader-toggle"><input type="checkbox" v-model="glossaryEnabled"> {{ $t('transcribe.glossary') }}</label>
          </div>
          <div class="glossary-controls">
            <span v-if="glossaryLoading" role="status">{{ $t('transcribe.recognizing') }}</span>
            <span v-else-if="glossaryError" role="status">{{ $t('transcribe.unavailable') }} <button class="glossary-retry" @click="loadGlossary">{{ $t('transcribe.retry') }}</button></span>

            <details class="glossary-attribution"><summary>{{ $t('transcribe.aboutTerms') }}</summary><span>{{ $t('transcribe.source') }} <a href="https://developers.google.com/machine-learning/glossary" target="_blank" rel="noopener noreferrer">Google ML Glossary</a>（<a href="https://creativecommons.org/licenses/by/4.0/" target="_blank" rel="noopener noreferrer">CC BY 4.0</a>）; {{ $t('transcribe.sourceNote') }}</span></details>
          </div>
        </div>

        <section v-if="summaryVisible" class="summary-panel" :aria-label="$t('transcribe.summary')" aria-live="polite">
          <div class="summary-heading"><h2>{{ $t('transcribe.summary') }}</h2><button class="export-btn" @click="summaryVisible = false" :aria-label="$t('transcribe.closeSummary')">×</button></div>
          <p v-if="summaryLoading" role="status">{{ $t('transcribe.summaryPending') }}</p>
          <p v-else-if="summaryError" role="alert">{{ $te(summaryError) ? $t(summaryError) : summaryError }} <button class="export-btn" @click="generateSummary">{{ $t('transcribe.retry') }}</button></p>
          <div v-else class="summary-text">{{ summary }}</div>
        </section>

        <div class="transcript-content" ref="transcriptContent" tabindex="0" :aria-label="$t('transcribe.transcript')" :style="{ '--reading-font-size': fontSize + 'px' }" @click="handleTranscriptClick" @wheel.passive="pauseFollowing" @touchmove.passive="pauseFollowing" @keydown="handleReadingKey" @pointerdown="handleReadingPointer">
          <div class="reading-column">
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
                @click.stop="handleSentenceClick($event, sentence)"
              ><GlossaryText :text="sentence.text + ' '" :spans="glossaryEnabled ? (glossarySpans[sentence.id] || []) : []" :highlights="highlights.filter(h => h.sentenceId === sentence.id)" /></span>
            </p>
          </div>
          </div>
        </div>

        <button v-if="!followPlayback && currentSentenceId != null" class="return-current" @click="resumeFollowing">{{ $t('transcribe.returnCurrent') }}</button>

        <!-- Highlight Toolbar -->
        <div
          v-if="showHighlightToolbar"
          class="highlight-toolbar"
          :style="{ top: toolbarPosition.top + 'px', left: toolbarPosition.left + 'px' }"
        >
          <button @click="addHighlight('yellow')" class="highlight-btn yellow" :title="$t('transcribe.yellow')">
            <span class="color-dot"></span>
          </button>
          <button @click="addHighlight('green')" class="highlight-btn green" :title="$t('transcribe.green')">
            <span class="color-dot"></span>
          </button>
          <button @click="addHighlight('blue')" class="highlight-btn blue" :title="$t('transcribe.blue')">
            <span class="color-dot"></span>
          </button>
          <button @click="addHighlight('pink')" class="highlight-btn pink" :title="$t('transcribe.pink')">
            <span class="color-dot"></span>
          </button>
        </div>

        <!-- Bottom floating stats -->
        <div class="reading-stats">
          <span>{{ $t('transcribe.words', { count: totalWords }) }}</span>
          <span class="dot">·</span>
          <span>{{ $t('transcribe.readTime', { count: readingTime }) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { trackPlayer, consumeLearningDetails } from '@/utils/learningAnalytics'
import axios from '../utils/axios'
import { setLocale } from '../i18n'
import { loadYouTubePlayer } from '../utils/youtubePlayer'
import { saveAs } from 'file-saver'
import GlossaryText from '../components/GlossaryText.vue'
import { readingParagraphs } from '../utils/readingParagraphs'

export default {
  name: 'TranscribeRead',
  components: { GlossaryText },
  data() {
    return {
      videoId: null,
      videoData: null,
      loading: true,
      loadingMessage: 'transcribe.loading',
      error: null,
      player: null,
      playerReady: false,
      playerError: '',
      disposed: false,
      currentTime: 0,
      currentSentenceId: null,
      timeUpdateInterval: null,
      fontSize: 14,
      followPlayback: true,
      focusReading: false,
      glossaryEnabled: true,
      glossarySpans: {},
      glossaryLoading: false,
      glossaryError: false,
      glossaryRequest: null,
      // Highlight feature
      highlights: [],
      showHighlightToolbar: false,
      toolbarPosition: { top: 0, left: 0 },
      pendingSelection: null,
      // Export feature
      summary: '',
      summaryVisible: false,
      summaryLoading: false,
      summaryError: '',
      showExportMenu: false
    }
  },
  computed: {
    glossaryKeywords() {
      const terms = new Map()
      for (const sentence of this.videoData?.sentences || []) {
        for (const term of this.glossarySpans[sentence.id] || []) {
          if (!term.termEn) continue
          const key = term.termId || term.termEn.toLowerCase()
          if (!terms.has(key)) terms.set(key, term)
        }
      }
      return [...terms.values()]
    },
    paragraphs() {
      return readingParagraphs(this.videoData?.sentences || [])
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
      this.error = 'transcribe.missingId'
      this.loading = false
      return
    }

    this.preparePlayer()
    await this.loadVideoData()
    if (this.disposed) return

    // Load saved highlights
    this.loadHighlights()

    // Listen for text selection
    document.addEventListener('mouseup', this.handleTextSelection)
    document.addEventListener('click', this.handleDocumentClick)
  },
  beforeUnmount() {
    this.disposed = true;
    this.stopLearningAnalytics?.();
    this.glossaryRequest?.abort()
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
    changeLocale: setLocale,
    async loadGlossary() {
      this.glossaryRequest?.abort()
      const request = new AbortController()
      this.glossaryRequest = request
      this.glossarySpans = {}
      this.glossaryLoading = true
      this.glossaryError = false
      const sentences = this.videoData?.sentences || []
      try {
        // Display completed batches immediately while keeping long transcript payloads bounded.
        for (let offset = 0; offset < sentences.length; offset += 80) {
          const batch = sentences.slice(offset, offset + 80)
          const { data } = await axios.post('/api/glossary/highlight', {
            texts: batch.map(sentence => sentence.text), language: this.$i18n.locale === 'zh-CN' ? 'zh' : 'en'
          }, { signal: request.signal, timeout: 30000 })
          if (this.disposed || request.signal.aborted) return
          if (!Array.isArray(data.results) || data.results.length !== batch.length) throw new Error('Invalid glossary response')
          batch.forEach((sentence, index) => {
            const result = data.results[index]
            if (result.text !== sentence.text || !Array.isArray(result.spans)) throw new Error('Invalid glossary offsets')
            this.glossarySpans[sentence.id] = result.spans
          })
        }
      } catch (error) {
        if (!request.signal.aborted && !this.disposed) this.glossaryError = true
      } finally {
        if (this.glossaryRequest === request && !this.disposed) this.glossaryLoading = false
      }
    },
    handleSentenceClick(event, sentence) {
      if (event.target.closest('.highlight')) this.handleTranscriptClick(event)
      else this.seekToSentence(sentence)
    },
    async generateSummary() {
      if (this.summaryLoading) return
      this.summaryVisible = true
      this.summaryError = ''
      this.summaryLoading = true
      try {
        const { data } = await axios.post(`/api/youtube/summary/${encodeURIComponent(this.videoId)}`, null, { timeout: 0 })
        this.summary = data.summary
      } catch (err) {
        this.summaryError = err.response?.data?.error || 'transcribe.summaryFailed'
      } finally {
        this.summaryLoading = false
      }
    },
    async loadVideoData() {
      try {
        this.loadingMessage = 'transcribe.checking'
        const statusRes = await axios.get(`/api/youtube/status/${this.videoId}`)

        if (statusRes.data.status === 'completed') {
          this.loadingMessage = 'transcribe.loading'
          const res = await axios.get(`/api/youtube/sentences/${this.videoId}`)
          if (this.disposed) return
          this.videoData = res.data
          if (Array.isArray(res.data.sentences) && res.data.sentences.length) window.xLearningAnalytics?.materialOpened('video')
          this.loading = false
          await this.$nextTick()
          this.initPlayer()
          this.loadGlossary()
        } else if (statusRes.data.status === 'parsing') {
          this.error = 'transcribe.parsing'
          this.loading = false
        } else if (statusRes.data.status === 'failed') {
          this.error = statusRes.data.message || 'transcribe.parseFailed'
          this.loading = false
        } else {
          this.error = 'transcribe.notParsed'
          this.loading = false
        }
      } catch (err) {
        console.error('Failed to load video data:', err)
        this.error = err.response?.data?.error || err.message || 'transcribe.loadError'
        this.loading = false
      }
    },

    async preparePlayer() {
      this.playerError = ''
      try {
        await loadYouTubePlayer()
        if (this.disposed) return
        await this.$nextTick()
        this.initPlayer()
      } catch (error) {
        if (!this.disposed) this.playerError = 'transcribe.playerUnavailable'
      }
    },

    initPlayer() {
      if (this.disposed || this.player || this.loading || !this.videoData || !window.YT?.Player) return

      this.player = new window.YT.Player('yt-player', {
        videoId: this.videoId,
        playerVars: {
          playsinline: 1,
          rel: 0,
          modestbranding: 1
        },
        events: {
          onReady: () => {
            if (this.disposed) return
            this.stopLearningAnalytics?.();
            this.stopLearningAnalytics = trackPlayer(this.player, consumeLearningDetails());
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
      const changed = found && found.id !== this.currentSentenceId
      if (found) this.currentSentenceId = found.id
      if (changed && this.followPlayback) this.$nextTick(this.scrollToCurrent)
    },

    pauseFollowing() { this.followPlayback = false },
    handleReadingKey(event) {
      if (['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End', ' '].includes(event.key)) this.pauseFollowing()
    },
    handleReadingPointer(event) {
      // Dragging the scrollbar is also an explicit request to browse independently.
      if (event.target === this.$refs.transcriptContent) this.pauseFollowing()
    },
    toggleFollow() {
      if (this.followPlayback) this.pauseFollowing()
      else this.resumeFollowing()
    },
    resumeFollowing() {
      this.followPlayback = true
      this.$nextTick(() => this.scrollToCurrent(true))
    },
    scrollToCurrent(force = false) {
      if (!this.followPlayback) return
      const container = this.$refs.transcriptContent
      const sentence = container?.querySelector('.sentence.current')
      if (!sentence) return
      const viewport = container.getBoundingClientRect()
      const rect = sentence.getBoundingClientRect()
      if (force || rect.top < viewport.top + 40 || rect.bottom > viewport.bottom - 60) {
        container.scrollTo({ top: container.scrollTop + rect.top - viewport.top - container.clientHeight * 0.35, behavior: 'smooth' })
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
        if (highlightId && confirm(this.$t('transcribe.deleteHighlight'))) {
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
      const title = this.videoData?.videoTitle || this.$t('transcribe.transcript')
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
        alert(this.$t('transcribe.exportFailed') + err.message)
      }
    },

    async exportPDF(title, filename) {
      const { jsPDF } = await import('jspdf')
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
      const { Document, Packer, Paragraph, TextRun, HeadingLevel } = await import('docx')
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

.keyword-panel { margin-top: 28px; padding: 20px; background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; }
.keyword-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.keyword-heading h2 { margin: 0; font-size: 16px; font-weight: 600; }
.keyword-heading > span { color: #6b7280; font-size: 12px; }
.keyword-hint, .keyword-status { margin: 10px 0 0; color: #6b7280; font-size: 12px; line-height: 1.6; }
.keyword-list { display: flex; flex-wrap: wrap; gap: 12px 10px; margin: 16px 0 0; padding: 0; list-style: none; }
.keyword-list li { max-width: 100%; font-size: 14px; line-height: 1.7; overflow-wrap: anywhere; }
.keyword-list :deep(.glossary-term) { padding: 3px 7px; box-decoration-break: clone; -webkit-box-decoration-break: clone; }

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

.header-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.export-btn:disabled { opacity: .6; cursor: wait; }
.summary-panel { margin: 16px 24px; padding: 18px; background: #f5f7ff; border: 1px solid #dce2f5; border-radius: 12px; max-height: 40vh; overflow-y: auto; flex-shrink: 0; }
.summary-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.summary-heading h2 { margin: 0 0 12px; font-size: 18px; }
.summary-text { white-space: pre-wrap; line-height: 1.8; overflow-wrap: anywhere; }
@media (max-width: 600px) { .header-top { flex-wrap: wrap; gap: 12px; } }

.glossary-controls { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 16px; margin-top: 12px; color: #64748b; font-size: 12px; line-height: 1.5; }
.glossary-controls label { display: inline-flex; gap: 6px; align-items: center; color: #2563a7; cursor: pointer; }
.glossary-controls input { accent-color: #2563a7; }
.glossary-attribution { flex-basis: 100%; font-size: 11px; }
.glossary-attribution a { color: #64748b; text-underline-offset: 2px; }
.glossary-retry { border: 0; background: transparent; color: #2563a7; text-decoration: underline; cursor: pointer; }

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
  background: transparent;
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
    overflow-y: visible;
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

<style scoped>
.right-panel { position: relative; min-width: 0; }
.transcript-header { padding: 22px 28px 12px; }
.transcript-title { font-size: 23px; }
.header-top { flex-wrap: wrap; }
.transcript-subtitle { margin-top: 8px; }
.reader-controls { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 16px; }
.reader-button { border: 1px solid #e5e7eb; border-radius: 7px; background: #fff; padding: 6px 10px; color: #475569; cursor: pointer; font: inherit; font-size: 12px; }
.reader-button[aria-pressed="true"] { color: #1d4ed8; background: #eff6ff; border-color: #bfdbfe; }
.reader-button:disabled { opacity: .4; cursor: default; }
.reader-button:focus-visible, .return-current:focus-visible { outline: 2px solid #2563eb; outline-offset: 3px; }
.font-controls { display: flex; gap: 6px; align-items: center; font-size: 12px; color: #64748b; }
.font-controls > span { min-width: 18px; text-align: center; }
.reader-toggle { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #64748b; cursor: pointer; }
.glossary-controls { margin-top: 8px; gap: 6px 12px; }
.glossary-attribution { flex-basis: auto; }
.glossary-attribution summary { cursor: pointer; }
.glossary-attribution > span { display: block; margin-top: 6px; max-width: 70ch; }
.transcript-content { padding: 28px 24px 96px; scroll-behavior: smooth; }
.reading-column { max-width: 72ch; margin: 0 auto; font-size: var(--reading-font-size, 14px); }
.paragraph { display: block; position: relative; padding: 12px 16px 18px; margin-bottom: 12px; cursor: default; }
.paragraph-time { display: block; width: auto; padding: 0; margin-bottom: 6px; font-size: 11px; color: #94a3b8; cursor: pointer; }
.paragraph-text { font-size: inherit; line-height: 1.9; color: #334155; overflow-wrap: anywhere; }
.paragraph.active { background: transparent; }
.sentence:hover { color: inherit; }
.sentence.current { background: #eff6ff; color: inherit; padding: 1px 0; margin: 0; }
.transcript-content :deep(.glossary-term) { background: #fef9c3; border-bottom-color: #e7ce6c; color: inherit; }
.transcript-content :deep(.glossary-term:hover), .transcript-content :deep(.glossary-term:focus-visible) { background: #fef08a; }
.return-current { position: absolute; bottom: 66px; left: 50%; transform: translateX(-50%); border: 1px solid #bfdbfe; background: #fff; color: #1d4ed8; box-shadow: 0 4px 18px #0f172a14; padding: 10px 18px; border-radius: 24px; cursor: pointer; white-space: nowrap; }
.focus-reading .right-panel { width: 100%; }
@media (prefers-reduced-motion: reduce) { .transcript-content { scroll-behavior: auto; } }
@media (max-width: 768px) {
  .right-panel { height: 85dvh; min-height: 480px; flex: none; }
  .focus-reading .right-panel { height: 100dvh; }
  .transcript-header { padding: 18px 16px 12px; }
  .transcript-content { padding: 18px 8px 80px; }
  .header-actions .export-btn { font-size: 12px; padding: 7px 9px; }
}
</style>
