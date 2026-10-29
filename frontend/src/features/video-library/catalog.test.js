import { test } from 'node:test'
import assert from 'node:assert/strict'
import { enrichVideo, filterVideos } from './catalog.js'
const filters = { category: 'all', difficulty: 'all', duration: 'all', path: '' }
const video = (videoId, title, duration, difficultyLevel = 'auto', previewAllowed = false) => enrichVideo({ videoId, title, duration, difficultyLevel, previewAllowed })

test('classification uses word boundaries and prioritises specific AI topics', () => {
  assert.equal(video('a', 'Daily routine', 500).category, 'other')
  assert.equal(video('b', 'Building AI agents with LLMs', 500).category, 'agents')
  assert.equal(video('c', 'Understanding GPT and transformers', 500).category, 'models')
  assert.equal(video('d', 'VLOG: morning routine', 500).category, 'english')
  assert.equal(video('e', 'Introduction to artificial intelligence', 500).category, 'basics')
})
test('unknown difficulty is not silently presented as beginner', () => {
  assert.equal(video('a', 'AI for beginners', 600).difficulty, 'unknown')
  assert.equal(video('a', 'AI basics', 600, 'easy').difficulty, 'easy')
})
test('duration boundaries and unknown durations are handled explicitly', () => {
  const videos = [0, 600, 601, 1200, 1201].map((duration, i) => video(String(i), 'AI', duration))
  const results = duration => filterVideos(videos, { ...filters, duration }).map(v => v.duration)
  assert.deepEqual(results('short'), [600])
  assert.deepEqual(results('medium'), [601, 1200])
  assert.deepEqual(results('long'), [1201])
})
test('curated paths use explicit production IDs and exclude failed videos', () => {
  const videos = [
    { ...video('LCEmiRjPEtQ', 'Software Is Changing', 2371), status: 'completed' },
    { ...video('AVQzG2MY858', 'LLM vs SLM', 849), status: 'completed' },
    { ...video('yAvJ7b_FxUA', 'Post-Training', 1708), status: 'failed' },
    { ...video('unreviewed', 'AI models', 300), status: 'completed' }
  ]
  assert.deepEqual(filterVideos(videos, { ...filters, path: 'foundations' }).map(v => v.videoId), ['AVQzG2MY858', 'LCEmiRjPEtQ'])
  assert.equal(videos[0].videoId, 'LCEmiRjPEtQ')
})
test('filtering and reordering never grants guest preview access', () => {
  const videos = [video('free', 'AI basics', 300, 'easy', true), video('locked', 'AI agents', 300, 'easy')]
  const [result] = filterVideos(videos, { ...filters, category: 'agents' })
  assert.equal(result.previewAllowed, false)
})
test('editorial goals override automatic classification for reviewed videos', () => {
  const result = video('Pz68t3RGeqI', 'Different title', 643)
  assert.equal(result.category, 'english')
  assert.ok(result.objective['zh-CN'])
  assert.equal(result.editorial, true)
})
