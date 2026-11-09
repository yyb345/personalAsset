import test from 'node:test'
import assert from 'node:assert/strict'
import { trackPlayer, learningDetails } from './learningAnalytics.js'

function playback() {
  let time = 0, position = 0, state = 2, visible = true, rate = 1, sample, canceled = false
  const events = []
  const dispose = trackPlayer({ getCurrentTime: () => position, getPlayerState: () => state, getPlaybackRate: () => rate }, {}, {
    now: () => time, visible: () => visible, emit: (name, params) => events.push({ name, ...params }),
    schedule: fn => { sample = fn; return 42 }, cancel: id => { canceled = id === 42 }
  })
  return {
    events, dispose, canceled: () => canceled,
    tick(options = {}) {
      time += (options.elapsed ?? 1) * 1000
      position += options.moved ?? (state === 1 ? rate : 0)
      if (options.state != null) state = options.state
      if (options.visible != null) visible = options.visible
      if (options.rate != null) rate = options.rate
      sample()
    }
  }
}
test('only actual playback starts a video, with one engagement and one event per milestone', () => {
  const p = playback()
  p.tick(); assert.equal(p.events.length, 0)
  p.tick({ state: 1 })
  for (let i = 0; i < 610; i++) p.tick()
  assert.deepEqual(p.events.map(e => e.name), ['video_start', 'learning_engaged', 'learning_milestone', 'learning_milestone'])
  assert.deepEqual(p.events.slice(1).map(e => e.learning_seconds), [60, 180, 600])
  p.dispose(); assert.equal(p.canceled(), true)
})
test('pauses, seeks, hidden playback and timer suspension do not count as learning', () => {
  const p = playback()
  p.tick({ state: 1 })
  for (let i = 0; i < 30; i++) p.tick()
  p.tick({ moved: 500 })
  p.tick({ moved: -300 })
  p.tick({ elapsed: 120, moved: 120 })
  p.tick({ visible: false })
  for (let i = 0; i < 70; i++) p.tick()
  p.tick({ visible: true, state: 2 })
  for (let i = 0; i < 70; i++) p.tick()
  assert.equal(p.events.length, 1)
  p.tick({ state: 1 })
  for (let i = 0; i < 30; i++) p.tick()
  assert.equal(p.events.filter(e => e.name === 'learning_engaged').length, 1)
})
test('double-speed playback measures elapsed learning time rather than media position', () => {
  const p = playback()
  p.tick({ state: 1, rate: 2 })
  for (let i = 0; i < 30; i++) p.tick()
  assert.equal(p.events.length, 1)
  for (let i = 0; i < 30; i++) p.tick()
  assert.equal(p.events[1].name, 'learning_engaged')
})
test('missing or stalled media progress never accumulates time', () => {
  const p = playback()
  p.tick({ state: 1 })
  for (let i = 0; i < 100; i++) p.tick({ moved: 0 })
  assert.equal(p.events.length, 1)
})
test('content language comes from metadata, not titles or content', () => {
  assert.equal(learningDetails({ subtitleLanguage: 'zh_Hans' }).content_language, 'zh')
  assert.equal(learningDetails({ sourceLanguage: 'en-US', subtitleLanguage: 'zh' }).content_language, 'en')
  assert.equal(learningDetails({ title: 'private title' }).content_language, 'unknown')
})

test('purchase requires a matching backend-confirmed order, amount and currency', async () => {
  const { confirmedPurchase } = await import('./learningAnalytics.js')
  const paid = { id: 'order', sessionId: 'cs_returned', status: 'paid', amount: 9900, currency: 'cny' }
  assert.equal(confirmedPurchase([paid], 'cs_returned'), paid)
  assert.equal(confirmedPurchase([paid], 'cs_someone_else'), undefined)
  assert.equal(confirmedPurchase([paid], undefined), undefined)
  for (const change of [{ status: 'pending' }, { status: 'failed' }, { amount: 1 }, { currency: 'usd' }]) {
    assert.equal(confirmedPurchase([{ ...paid, ...change }], 'cs_returned'), undefined)
  }
})
