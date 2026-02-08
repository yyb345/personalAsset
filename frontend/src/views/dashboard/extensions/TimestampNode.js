import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import TimestampNodeView from './TimestampNodeView.vue'

export default Node.create({
  name: 'timestamp',
  group: 'inline',
  inline: true,
  atom: true,

  addAttributes() {
    return {
      seconds: {
        default: 0,
        parseHTML: element => Number(element.getAttribute('data-seconds')) || 0,
        renderHTML: attributes => ({ 'data-seconds': attributes.seconds })
      }
    }
  },

  parseHTML() {
    return [{ tag: 'span[data-type="timestamp"]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes({ 'data-type': 'timestamp' }, HTMLAttributes)]
  },

  addNodeView() {
    return VueNodeViewRenderer(TimestampNodeView)
  }
})
