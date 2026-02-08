export function createSlashCommandsPopup() {
  let popup = null
  let selectedIndex = 0
  let items = []
  let commandFn = null

  function render() {
    if (!popup) return
    const children = popup.querySelectorAll('.slash-item')
    children.forEach((el, i) => {
      el.classList.toggle('is-selected', i === selectedIndex)
    })
  }

  function selectItem(index) {
    const item = items[index]
    if (item && commandFn) {
      commandFn(item)
    }
  }

  return {
    onStart(props) {
      items = props.items
      commandFn = props.command

      popup = document.createElement('div')
      popup.className = 'slash-commands-popup'

      items.forEach((item, index) => {
        const el = document.createElement('button')
        el.className = 'slash-item'
        el.type = 'button'
        el.innerHTML = `<span class="slash-icon">${item.icon}</span><span class="slash-title">${item.title}</span>`
        el.addEventListener('mousedown', (e) => {
          e.preventDefault()
          selectItem(index)
        })
        el.addEventListener('mouseenter', () => {
          selectedIndex = index
          render()
        })
        popup.appendChild(el)
      })

      document.body.appendChild(popup)
      selectedIndex = 0
      render()

      if (props.clientRect) {
        const rect = props.clientRect()
        if (rect) {
          popup.style.position = 'fixed'
          popup.style.left = `${rect.left}px`
          popup.style.top = `${rect.bottom + 4}px`
        }
      }
    },

    onUpdate(props) {
      items = props.items
      commandFn = props.command

      if (!popup) return
      popup.innerHTML = ''

      items.forEach((item, index) => {
        const el = document.createElement('button')
        el.className = 'slash-item'
        el.type = 'button'
        el.innerHTML = `<span class="slash-icon">${item.icon}</span><span class="slash-title">${item.title}</span>`
        el.addEventListener('mousedown', (e) => {
          e.preventDefault()
          selectItem(index)
        })
        el.addEventListener('mouseenter', () => {
          selectedIndex = index
          render()
        })
        popup.appendChild(el)
      })

      selectedIndex = 0
      render()

      if (props.clientRect) {
        const rect = props.clientRect()
        if (rect) {
          popup.style.left = `${rect.left}px`
          popup.style.top = `${rect.bottom + 4}px`
        }
      }
    },

    onKeyDown(props) {
      const { event } = props
      if (event.key === 'ArrowUp') {
        selectedIndex = (selectedIndex + items.length - 1) % items.length
        render()
        return true
      }
      if (event.key === 'ArrowDown') {
        selectedIndex = (selectedIndex + 1) % items.length
        render()
        return true
      }
      if (event.key === 'Enter') {
        selectItem(selectedIndex)
        return true
      }
      if (event.key === 'Escape') {
        if (popup) {
          popup.remove()
          popup = null
        }
        return true
      }
      return false
    },

    onExit() {
      if (popup) {
        popup.remove()
        popup = null
      }
    }
  }
}
