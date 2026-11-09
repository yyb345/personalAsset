// Only categorical metadata leaves the app. No titles, private IDs or transcript content.
export function learningDetails(video = {}, entryPoint = 'direct') {
  const language = String(video.sourceLanguage || video.videoLanguage || video.subtitleLanguage || video.language || '').toLowerCase()
  return {
    content_language: /^(zh|cmn|yue)([-_]|$)/.test(language) ? 'zh' : /^en([-_]|$)/.test(language) ? 'en' : 'unknown',
    content_category: video.category,
    entry_point: entryPoint
  }
}

export function track(name, details) {
  try { window.xLearningAnalytics?.event(name, details) } catch { /* Analytics must never interrupt learning or payment. */ }
}

// Samples actual playback, rejecting seeks, pauses, background playback and suspended timers.
// One tracker lives for one material visit; callers dispose it before replacing the player.
export function trackPlayer(player, details = {}, env = {}) {
  const now = env.now || (() => performance.now())
  const visible = env.visible || (() => document.visibilityState === 'visible')
  const emit = env.emit || track
  const schedule = env.schedule || (fn => setInterval(fn, 1000))
  const cancel = env.cancel || clearInterval
  let previous = null, seconds = 0, started = false
  const milestones = new Set()
  function sample() {
    try {
      const current = { time: now(), position: player.getCurrentTime(), playing: player.getPlayerState() === 1, visible: visible(), rate: player.getPlaybackRate?.() || 1 }
      if (current.playing && current.visible && !started) {
        started = true
        emit('video_start', details)
      }
      if (previous && current.playing && previous.playing && current.visible && previous.visible) {
        const elapsed = (current.time - previous.time) / 1000
        const moved = current.position - previous.position
        if (elapsed > 0 && elapsed <= 2.5 && moved > 0 && moved <= elapsed * current.rate + 0.5) {
          seconds += Math.min(elapsed, moved / current.rate)
          for (const threshold of [60, 180, 600]) {
            if (seconds >= threshold && !milestones.has(threshold)) {
              milestones.add(threshold)
              emit(threshold === 60 ? 'learning_engaged' : 'learning_milestone', { ...details, learning_seconds: threshold })
            }
          }
        }
      }
      previous = current
    } catch { previous = null }
  }
  const reset = () => { previous = null }
  // Reset immediately so a quick background/foreground transition cannot count hidden time.
  if (!env.visible) document.addEventListener('visibilitychange', reset)
  const timer = schedule(sample)
  return () => {
    cancel(timer)
    if (!env.visible) document.removeEventListener('visibilitychange', reset)
  }
}

let selectedDetails
export function selectVideo(video, entryPoint) {
  selectedDetails = learningDetails(video, entryPoint)
  track('video_selected', selectedDetails)
}
export function consumeLearningDetails() {
  const details = selectedDetails || learningDetails()
  selectedDetails = undefined
  return details
}

// Call only with records from the authenticated billing API, never URL-derived status.
export function confirmedPurchase(payments, sessionId) {
  if (typeof sessionId !== 'string' || !sessionId || !Array.isArray(payments)) return undefined
  return payments.find(payment => payment.sessionId === sessionId && payment.status === 'paid'
    && payment.amount === 99 && payment.currency === 'usd')
}
