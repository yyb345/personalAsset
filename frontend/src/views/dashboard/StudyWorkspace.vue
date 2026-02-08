<template>
  <div class="study-workspace" @timestamp-click="onTimestampClick">
    <!-- Left Panel -->
    <div class="sw-left" :style="{ width: leftWidth + 'px' }">
      <div class="sw-player-wrapper">
        <div id="sw-youtube-player"></div>
      </div>

      <!-- Notes sidebar -->
      <div class="sw-notes-sidebar">
        <div class="sw-notes-header">
          <h3>Saved Notes</h3>
          <button class="sw-btn-new" @click="createNewNote">+ New</button>
        </div>
        <div v-if="savedNotes.length === 0" class="sw-empty">No notes yet</div>
        <div
          v-for="note in savedNotes"
          :key="note.id"
          :class="['sw-note-item', { active: currentNoteId === note.id }]"
          @click="loadNote(note)"
        >
          <div class="sw-note-title">{{ note.title || 'Untitled' }}</div>
          <div class="sw-note-date">{{ formatDate(note.updatedAt) }}</div>
          <button class="sw-note-delete" @click.stop="deleteNote(note.id)" title="Delete">×</button>
        </div>
      </div>
    </div>

    <!-- Resizer -->
    <div class="sw-resizer" @mousedown="startResize" @touchstart.prevent="startResize">
      <div class="sw-resizer-line"></div>
    </div>

    <!-- Right Panel -->
    <div class="sw-right">
      <!-- Title -->
      <input
        v-model="noteTitle"
        class="sw-title-input"
        placeholder="Note title..."
        @input="scheduleSave"
      />

      <!-- Toolbar -->
      <div class="sw-toolbar" v-if="editor">
        <button
          :class="{ active: editor.isActive('bold') }"
          @click="editor.chain().focus().toggleBold().run()"
          title="Bold"
        ><b>B</b></button>
        <button
          :class="{ active: editor.isActive('italic') }"
          @click="editor.chain().focus().toggleItalic().run()"
          title="Italic"
        ><i>I</i></button>
        <button
          :class="{ active: editor.isActive('underline') }"
          @click="editor.chain().focus().toggleUnderline().run()"
          title="Underline"
        ><u>U</u></button>
        <button
          :class="{ active: editor.isActive('strike') }"
          @click="editor.chain().focus().toggleStrike().run()"
          title="Strikethrough"
        ><s>S</s></button>
        <span class="sw-toolbar-divider"></span>
        <button
          :class="{ active: editor.isActive('heading', { level: 1 }) }"
          @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
          title="Heading 1"
        >H1</button>
        <button
          :class="{ active: editor.isActive('heading', { level: 2 }) }"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
          title="Heading 2"
        >H2</button>
        <button
          :class="{ active: editor.isActive('heading', { level: 3 }) }"
          @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
          title="Heading 3"
        >H3</button>
        <span class="sw-toolbar-divider"></span>
        <button
          :class="{ active: editor.isActive('bulletList') }"
          @click="editor.chain().focus().toggleBulletList().run()"
          title="Bullet List"
        >&#8226;</button>
        <button
          :class="{ active: editor.isActive('orderedList') }"
          @click="editor.chain().focus().toggleOrderedList().run()"
          title="Numbered List"
        >1.</button>
        <button
          :class="{ active: editor.isActive('taskList') }"
          @click="editor.chain().focus().toggleTaskList().run()"
          title="Checklist"
        >&#9745;</button>
        <button
          :class="{ active: editor.isActive('blockquote') }"
          @click="editor.chain().focus().toggleBlockquote().run()"
          title="Quote"
        >&ldquo;</button>
        <button
          :class="{ active: editor.isActive('codeBlock') }"
          @click="editor.chain().focus().toggleCodeBlock().run()"
          title="Code Block"
        >&lt;/&gt;</button>
        <span class="sw-toolbar-divider"></span>
        <button class="sw-btn-timestamp" @click="insertTimestamp" title="Insert Timestamp">
          &#9201; Timestamp
        </button>
        <div class="sw-toolbar-right">
          <span v-if="saveStatus" class="sw-save-status">{{ saveStatus }}</span>
          <button class="sw-btn-save" @click="saveNote">Save</button>
          <div class="sw-export-wrapper">
            <button class="sw-btn-export" @click="showExport = !showExport">Export &#9662;</button>
            <div v-if="showExport" class="sw-export-menu">
              <button @click="exportMarkdown">Markdown</button>
              <button @click="exportHtml">HTML</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Editor -->
      <div class="sw-editor-container">
        <editor-content :editor="editor" class="sw-editor" />
      </div>
    </div>
  </div>
</template>

<script>
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Placeholder from '@tiptap/extension-placeholder'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { common, createLowlight } from 'lowlight'
import TimestampNode from './extensions/TimestampNode'
import SlashCommands from './extensions/SlashCommands'
import axios from '../../utils/axios'

const lowlight = createLowlight(common)

export default {
  name: 'StudyWorkspace',
  components: { EditorContent },
  data() {
    return {
      editor: null,
      ytPlayer: null,
      ytReady: false,
      noteTitle: '',
      currentNoteId: null,
      savedNotes: [],
      saveStatus: '',
      saveTimer: null,
      showExport: false,
      leftWidth: 480,
      isResizing: false
    }
  },
  computed: {
    videoId() {
      return this.$route.params.videoId || ''
    }
  },
  mounted() {
    this.initEditor()
    this.loadYoutubeAPI()
    this.loadNotes()
    document.addEventListener('click', this.closeExport)
  },
  beforeUnmount() {
    if (this.editor) this.editor.destroy()
    if (this.ytPlayer) this.ytPlayer.destroy()
    if (this.saveTimer) clearTimeout(this.saveTimer)
    document.removeEventListener('mousemove', this.handleResize)
    document.removeEventListener('mouseup', this.stopResize)
    document.removeEventListener('touchmove', this.handleResize)
    document.removeEventListener('touchend', this.stopResize)
    document.removeEventListener('click', this.closeExport)
  },
  methods: {
    initEditor() {
      this.editor = new Editor({
        extensions: [
          StarterKit.configure({ codeBlock: false }),
          Underline,
          Placeholder.configure({ placeholder: 'Start taking notes... Type / for commands' }),
          TaskList,
          TaskItem.configure({ nested: true }),
          CodeBlockLowlight.configure({ lowlight }),
          TimestampNode,
          SlashCommands.configure({ timestampFn: this.insertTimestamp })
        ],
        onUpdate: () => {
          this.scheduleSave()
        }
      })
    },

    // YouTube Player
    loadYoutubeAPI() {
      if (window.YT && window.YT.Player) {
        this.createPlayer()
        return
      }
      const existing = document.querySelector('script[src*="youtube.com/iframe_api"]')
      if (!existing) {
        const tag = document.createElement('script')
        tag.src = 'https://www.youtube.com/iframe_api'
        document.head.appendChild(tag)
      }
      const orig = window.onYouTubeIframeAPIReady
      window.onYouTubeIframeAPIReady = () => {
        if (orig) orig()
        this.createPlayer()
      }
    },

    createPlayer() {
      if (!this.videoId) return
      this.ytPlayer = new window.YT.Player('sw-youtube-player', {
        height: '270',
        width: '100%',
        videoId: this.videoId,
        playerVars: { playsinline: 1, rel: 0, modestbranding: 1 },
        events: {
          onReady: () => { this.ytReady = true }
        }
      })
    },

    // Timestamp
    insertTimestamp() {
      if (!this.editor) return
      let seconds = 0
      if (this.ytPlayer && this.ytReady && typeof this.ytPlayer.getCurrentTime === 'function') {
        seconds = Math.floor(this.ytPlayer.getCurrentTime())
      }
      this.editor.chain().focus().insertContent({
        type: 'timestamp',
        attrs: { seconds }
      }).insertContent(' ').run()
    },

    onTimestampClick(e) {
      const seconds = e.detail?.seconds
      if (seconds != null && this.ytPlayer && this.ytReady) {
        this.ytPlayer.seekTo(seconds, true)
        this.ytPlayer.playVideo()
      }
    },

    // Notes CRUD
    async loadNotes() {
      if (!this.videoId) return
      try {
        const res = await axios.get(`/api/study-notes/video/${this.videoId}`)
        this.savedNotes = res.data.notes || []
        if (this.savedNotes.length > 0 && !this.currentNoteId) {
          this.loadNote(this.savedNotes[0])
        }
      } catch (e) {
        console.error('Failed to load notes:', e)
      }
    },

    loadNote(note) {
      this.currentNoteId = note.id
      this.noteTitle = note.title || ''
      if (note.contentJson) {
        try {
          this.editor.commands.setContent(JSON.parse(note.contentJson))
        } catch {
          this.editor.commands.setContent('')
        }
      } else {
        this.editor.commands.setContent('')
      }
      this.saveStatus = ''
    },

    createNewNote() {
      this.currentNoteId = null
      this.noteTitle = ''
      this.editor.commands.clearContent()
      this.saveStatus = ''
    },

    scheduleSave() {
      if (this.saveTimer) clearTimeout(this.saveTimer)
      this.saveStatus = 'Unsaved'
      this.saveTimer = setTimeout(() => {
        this.saveNote()
      }, 3000)
    },

    async saveNote() {
      if (this.saveTimer) clearTimeout(this.saveTimer)
      if (!this.editor) return

      const contentJson = JSON.stringify(this.editor.getJSON())
      const contentHtml = this.editor.getHTML()
      const title = this.noteTitle || 'Untitled'

      try {
        if (this.currentNoteId) {
          await axios.put(`/api/study-notes/${this.currentNoteId}`, {
            title, contentJson, contentHtml
          })
        } else {
          const res = await axios.post('/api/study-notes', {
            videoId: this.videoId, title, contentJson, contentHtml
          })
          this.currentNoteId = res.data.note.id
        }
        this.saveStatus = 'Saved'
        setTimeout(() => { if (this.saveStatus === 'Saved') this.saveStatus = '' }, 2000)
        this.loadNotes()
      } catch (e) {
        console.error('Save failed:', e)
        this.saveStatus = 'Save failed'
      }
    },

    async deleteNote(id) {
      if (!confirm('Delete this note?')) return
      try {
        await axios.delete(`/api/study-notes/${id}`)
        if (this.currentNoteId === id) {
          this.createNewNote()
        }
        this.loadNotes()
      } catch (e) {
        console.error('Delete failed:', e)
      }
    },

    // Export
    exportMarkdown() {
      this.showExport = false
      const html = this.editor.getHTML()
      // Simple HTML-to-markdown conversion
      let md = html
        .replace(/<h1[^>]*>(.*?)<\/h1>/gi, '# $1\n\n')
        .replace(/<h2[^>]*>(.*?)<\/h2>/gi, '## $1\n\n')
        .replace(/<h3[^>]*>(.*?)<\/h3>/gi, '### $1\n\n')
        .replace(/<strong>(.*?)<\/strong>/gi, '**$1**')
        .replace(/<em>(.*?)<\/em>/gi, '*$1*')
        .replace(/<u>(.*?)<\/u>/gi, '$1')
        .replace(/<s>(.*?)<\/s>/gi, '~~$1~~')
        .replace(/<li[^>]*>(.*?)<\/li>/gi, '- $1\n')
        .replace(/<blockquote[^>]*>(.*?)<\/blockquote>/gi, '> $1\n\n')
        .replace(/<span data-type="timestamp" data-seconds="(\d+)"[^>]*>[^<]*<\/span>/gi, '[$1s]')
        .replace(/<p[^>]*>(.*?)<\/p>/gi, '$1\n\n')
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/<[^>]+>/g, '')
        .replace(/\n{3,}/g, '\n\n')
        .trim()

      this.downloadFile(`${this.noteTitle || 'note'}.md`, md, 'text/markdown')
    },

    exportHtml() {
      this.showExport = false
      const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${this.noteTitle || 'Note'}</title></head><body>${this.editor.getHTML()}</body></html>`
      this.downloadFile(`${this.noteTitle || 'note'}.html`, html, 'text/html')
    },

    downloadFile(filename, content, type) {
      const blob = new Blob([content], { type })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
    },

    closeExport(e) {
      if (this.showExport && !e.target.closest('.sw-export-wrapper')) {
        this.showExport = false
      }
    },

    // Resize
    startResize(e) {
      this.isResizing = true
      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
      document.addEventListener('mousemove', this.handleResize)
      document.addEventListener('mouseup', this.stopResize)
      document.addEventListener('touchmove', this.handleResize)
      document.addEventListener('touchend', this.stopResize)
    },

    handleResize(e) {
      if (!this.isResizing) return
      const clientX = e.touches ? e.touches[0].clientX : e.clientX
      const container = this.$el.getBoundingClientRect()
      const newWidth = Math.min(Math.max(clientX - container.left, 300), container.width - 400)
      this.leftWidth = newWidth
    },

    stopResize() {
      this.isResizing = false
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
      document.removeEventListener('mousemove', this.handleResize)
      document.removeEventListener('mouseup', this.stopResize)
      document.removeEventListener('touchmove', this.handleResize)
      document.removeEventListener('touchend', this.stopResize)
    },

    // Helpers
    formatDate(dateStr) {
      if (!dateStr) return ''
      const d = new Date(dateStr)
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
    }
  }
}
</script>

<style>
@import '../../assets/styles/study-workspace.css';
</style>
