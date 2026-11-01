let pending

// Share one script request across detail-page visits; allow retry after failure.
export function loadYouTubePlayer() {
  if (window.YT?.Player) return Promise.resolve()
  if (pending) return pending

  pending = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    const previous = window.onYouTubeIframeAPIReady
    const finish = (error) => {
      clearTimeout(timer)
      script.onerror = null
      if (window.onYouTubeIframeAPIReady === ready) {
        window.onYouTubeIframeAPIReady = previous
      }
      if (error) {
        script.remove()
        reject(error)
      } else {
        resolve()
      }
    }
    const ready = () => {
      finish()
      if (typeof previous === 'function') previous()
    }
    const timer = setTimeout(() => finish(new Error('YouTube player timed out')), 10000)
    window.onYouTubeIframeAPIReady = ready
    script.src = 'https://www.youtube.com/iframe_api'
    script.onerror = () => finish(new Error('YouTube player failed to load'))
    document.head.appendChild(script)
  }).catch(error => {
    pending = undefined
    throw error
  })
  return pending
}
