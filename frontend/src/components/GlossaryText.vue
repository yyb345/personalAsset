<template>
  <span><template v-for="part in parts" :key="part.start"><span
    v-if="part.term"
    class="glossary-term"
    tabindex="0"
    :aria-describedby="activeTerm === part.term ? tooltipId : undefined"
    @mouseenter="showTerm($event, part.term)"
    @mouseleave="scheduleClose"
    @focus="showTerm($event, part.term)"
    @blur="scheduleClose"
    @keydown.esc.stop="close"
  ><mark v-if="part.highlight" :class="markClass(part.highlight)" :data-id="part.highlight.id">{{ part.text }}</mark><template v-else>{{ part.text }}</template></span><mark
    v-else-if="part.highlight" :class="markClass(part.highlight)" :data-id="part.highlight.id"
  >{{ part.text }}</mark><template v-else>{{ part.text }}</template></template></span>
  <Teleport to="body">
    <div v-if="activeTerm" :id="tooltipId" role="tooltip" class="glossary-tooltip" :style="tooltipStyle" @mouseenter="cancelClose" @mouseleave="close">
      <strong>{{ activeTerm.termEn }}<template v-if="activeTerm.termZh"> · {{ activeTerm.termZh }}</template></strong>
      <p v-if="activeTerm.definitionZh" lang="zh">{{ activeTerm.definitionZh }}</p>
      <p v-if="activeTerm.definitionEn" lang="en">{{ activeTerm.definitionEn }}</p>
    </div>
  </Teleport>
</template>

<script>
import { transcriptSegments } from '../utils/transcriptSegments'
let nextTooltipId = 0
export default {
  props: {
    text: { type: String, default: '' },
    spans: { type: Array, default: () => [] },
    highlights: { type: Array, default: () => [] }
  },
  data: () => ({ activeTerm: null, activeAnchor: null, tooltipId: `glossary-tooltip-${++nextTooltipId}`, tooltipStyle: {}, closeTimer: null }),
  computed: { parts() { return transcriptSegments(this.text, this.spans, this.highlights) } },
  watch: {
    spans() { this.close() },
    text() { this.close() }
  },
  beforeUnmount() {
    this.close()
    window.removeEventListener('scroll', this.positionTooltip, true)
    window.removeEventListener('resize', this.positionTooltip)
  },
  methods: {
    markClass(highlight) {
      const color = ['yellow', 'green', 'blue', 'pink'].includes(highlight.color) ? highlight.color : 'yellow'
      return ['highlight', `highlight-${color}`]
    },
    async showTerm(event, term) {
      this.cancelClose()
      this.activeAnchor = event.currentTarget
      this.activeTerm = term
      window.addEventListener('scroll', this.positionTooltip, true)
      window.addEventListener('resize', this.positionTooltip)
      await this.positionTooltip()
    },
    async positionTooltip() {
      if (!this.activeTerm || !this.activeAnchor) return
      const rect = this.activeAnchor.getBoundingClientRect()
      const width = Math.min(360, window.innerWidth - 24)
      this.tooltipStyle = { width: `${width}px`, left: `${Math.max(12, Math.min(rect.left, window.innerWidth - width - 12))}px`, top: `${rect.bottom + 8}px` }
      await this.$nextTick()
      const tooltip = document.getElementById(this.tooltipId)
      if (tooltip && rect.bottom + 8 + tooltip.offsetHeight > window.innerHeight - 12) {
        this.tooltipStyle.top = `${Math.max(12, rect.top - tooltip.offsetHeight - 8)}px`
      }
    },
    cancelClose() { clearTimeout(this.closeTimer) },
    scheduleClose() { this.cancelClose(); this.closeTimer = setTimeout(this.close, 160) },
    close() {
      this.cancelClose()
      this.activeTerm = null
      this.activeAnchor = null
      window.removeEventListener('scroll', this.positionTooltip, true)
      window.removeEventListener('resize', this.positionTooltip)
    }
  }
}
</script>

<style scoped>
.glossary-term { color: #2563a7; background: #eff6ff; border-bottom: 1px dotted #60a5fa; border-radius: 3px; cursor: help; }
.glossary-term:hover, .glossary-term:focus-visible { background: #dbeafe; outline: 2px solid #93c5fd; outline-offset: 2px; }
.glossary-tooltip { position: fixed; z-index: 10000; padding: 14px 16px; background: #172438; color: #f8fafc; border: 1px solid #334155; border-radius: 10px; box-shadow: 0 8px 28px #0f172a33; font-size: 13px; line-height: 1.65; max-height: calc(100vh - 24px); overflow: auto; overflow-wrap: anywhere; }
.glossary-tooltip strong { font-size: 14px; color: #bfdbfe; }
.glossary-tooltip p { margin: 8px 0 0; }
</style>
