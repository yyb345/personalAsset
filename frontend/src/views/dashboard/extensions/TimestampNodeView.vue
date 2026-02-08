<template>
  <node-view-wrapper as="span" class="timestamp-chip" @click="handleClick">
    {{ formattedTime }}
  </node-view-wrapper>
</template>

<script>
import { NodeViewWrapper } from '@tiptap/vue-3'

export default {
  name: 'TimestampNodeView',
  components: { NodeViewWrapper },
  props: {
    node: { type: Object, required: true }
  },
  computed: {
    formattedTime() {
      const total = Math.floor(this.node.attrs.seconds)
      const hrs = Math.floor(total / 3600)
      const mins = Math.floor((total % 3600) / 60)
      const secs = total % 60
      if (hrs > 0) {
        return `${hrs}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
      }
      return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    }
  },
  methods: {
    handleClick() {
      const event = new CustomEvent('timestamp-click', {
        detail: { seconds: this.node.attrs.seconds },
        bubbles: true
      })
      this.$el.dispatchEvent(event)
    }
  }
}
</script>
