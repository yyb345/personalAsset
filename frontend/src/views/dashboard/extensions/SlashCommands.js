import { Extension } from '@tiptap/core'
import Suggestion from '@tiptap/suggestion'
import { createSlashCommandsPopup } from './SlashCommandsPopup'

const slashItems = [
  { title: 'Heading 1', icon: 'H1', command: ({ editor }) => editor.chain().focus().toggleHeading({ level: 1 }).run() },
  { title: 'Heading 2', icon: 'H2', command: ({ editor }) => editor.chain().focus().toggleHeading({ level: 2 }).run() },
  { title: 'Heading 3', icon: 'H3', command: ({ editor }) => editor.chain().focus().toggleHeading({ level: 3 }).run() },
  { title: 'Bullet List', icon: '•', command: ({ editor }) => editor.chain().focus().toggleBulletList().run() },
  { title: 'Numbered List', icon: '1.', command: ({ editor }) => editor.chain().focus().toggleOrderedList().run() },
  { title: 'Checklist', icon: '☑', command: ({ editor }) => editor.chain().focus().toggleTaskList().run() },
  { title: 'Code Block', icon: '<>', command: ({ editor }) => editor.chain().focus().toggleCodeBlock().run() },
  { title: 'Blockquote', icon: '"', command: ({ editor }) => editor.chain().focus().toggleBlockquote().run() },
  { title: 'Timestamp', icon: '⏱', command: ({ editor, timestampFn }) => { if (timestampFn) timestampFn() } }
]

export default Extension.create({
  name: 'slashCommands',

  addOptions() {
    return {
      suggestion: {
        char: '/',
        command: ({ editor, range, props }) => {
          editor.chain().focus().deleteRange(range).run()
          props.command({ editor, timestampFn: this.options.timestampFn })
        },
        items: ({ query }) => {
          return slashItems.filter(item =>
            item.title.toLowerCase().includes(query.toLowerCase())
          )
        },
        render: createSlashCommandsPopup
      },
      timestampFn: null
    }
  },

  addProseMirrorPlugins() {
    return [
      Suggestion({
        editor: this.editor,
        ...this.options.suggestion
      })
    ]
  }
})
