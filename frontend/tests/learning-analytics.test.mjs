import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
const source = readFileSync(new URL('../public/js/learning-analytics.js', import.meta.url), 'utf8')
function browser(path = '/', referrer = '', storage = new Map(), hostname = 'www.xlearning.top') {
  const listeners = {}
  const scripts = []
  const window = { location: { pathname: path, origin: 'https://www.xlearning.top', hostname }, sessionStorage: { getItem: k => storage.get(k), setItem: (k, v) => storage.set(k, v) } }
  const document = { referrer, createElement: () => ({}), head: { appendChild(script) { scripts.push(script) } }, addEventListener: (name, fn) => { listeners[name] = fn } }
  vm.runInNewContext(source, { window, document, URL, Date })
  const events = name => window.dataLayer.filter(args => args[0] === 'event' && args[1] === name).map(args => args[2])
  return { window, events, listeners, storage, scripts }
}
test('initial page and Vue navigation produce exactly one view; query and private IDs never leave', () => {
  const b = browser('/transcribe', 'https://example.com/private?email=secret@example.com')
  b.window.xLearningAnalytics.page('/transcribe?videoId=secret&email=secret@example.com')
  assert.equal(b.events('page_view').length, 1)
  b.window.location.pathname = '/dashboard/study-workspace/private-id'
  b.window.xLearningAnalytics.page('/dashboard/study-workspace/private-id?token=secret')
  assert.equal(b.events('page_view')[1].page_path, '/dashboard/study-workspace/:videoId')
  assert(!JSON.stringify(b.window.dataLayer).includes('secret'))
  assert(!JSON.stringify(b.window.dataLayer).includes('private-id'))
  assert.equal(b.window.dataLayer[1][2].send_page_view, false)
})
test('search-referrer public landing is retained across documents through learning conversion', () => {
  const landing = browser('/guides/english-dictation-practice', 'https://www.google.com/search?q=private-query')
  assert.equal(landing.events('seo_landing').length, 1)
  landing.listeners.click({ button: 0, target: { closest: () => ({ getAttribute: () => 'video' }) } })
  assert.equal(landing.events('start_learning')[0].entry_source, 'search_referrer')
  const app = browser('/transcribe', 'https://www.xlearning.top/guides/english-dictation-practice', landing.storage)
  app.window.xLearningAnalytics.materialOpened('video')
  assert.equal(app.events('seo_landing').length, 0)
  assert.equal(app.events('material_opened')[0].landing_page, '/guides/english-dictation-practice')
  assert(!JSON.stringify(app.window.dataLayer).includes('private-query'))
})
test('untrusted content kinds, arbitrary paths and corrupt storage cannot become event values', () => {
  const storage = new Map([['xlearning.acquisition.v1', JSON.stringify({ landing_page: '/guides/email@example.com', entry_source: 'secret', expires: Date.now() + 60000 })]])
  const b = browser('/notes/email@example.com', '', storage)
  assert.equal(b.events('page_view')[0].page_path, '/other')
  b.window.xLearningAnalytics.materialOpened('private lesson title')
  assert.equal(b.events('material_opened').length, 0)
  b.window.xLearningAnalytics.startLearning('private lesson title')
  assert.equal(b.events('start_learning')[0].learning_type, 'workspace')
  assert(!JSON.stringify(b.window.dataLayer).includes('email@example.com'))
})
test('storage failure never blocks page or learning events', () => {
  const b = browser('/')
  b.window.sessionStorage.setItem = () => { throw new Error('blocked') }
  b.window.xLearningAnalytics.materialOpened('reading')
  assert.equal(b.events('material_opened').length, 1)
})
test('published public video URL remains measurable while workspace and invalid paths stay masked', () => {
  const b = browser('/video/dQw4w9WgXcQ')
  assert.equal(b.events('seo_landing')[0].landing_page, '/video/dQw4w9WgXcQ')
  b.window.xLearningAnalytics.page('/video/user@example.com')
  assert.equal(b.events('page_view')[1].page_path, '/other')
  assert(!JSON.stringify(b.window.dataLayer).includes('user@example.com'))
})

test('referrer retains only HTTP(S) origin, and local hosts never load production GA', () => {
  const b = browser('/', 'https://user:password@www.google.com/private?email=secret@example.com#token')
  assert.equal(b.events('page_view')[0].page_referrer, 'https://www.google.com/')
  assert.equal(b.scripts.length, 1)
  for (const host of ['localhost', '127.0.0.1', '[::1]', 'preview.example.com']) {
    const local = browser('/', 'javascript:private-data', new Map(), host)
    assert.equal(local.scripts.length, 0)
    assert.equal(local.events('page_view')[0].page_referrer, '')
    assert.equal(local.events('page_view').length, 1)
  }
})
test('expired attribution does not survive a new private app visit', () => {
  const storage = new Map([['xlearning.acquisition.v1', JSON.stringify({ landing_page: '/', entry_source: 'search_referrer', expires: Date.now() - 1 })]])
  const b = browser('/transcribe', '', storage)
  b.window.xLearningAnalytics.materialOpened('video')
  assert.equal(b.events('material_opened')[0].landing_page, 'none')
})

test('Chinese homepage keeps its canonical path and search landing attribution', () => {
  const b = browser('/zh/', 'https://www.google.com/')
  assert.equal(b.events('seo_landing')[0].landing_page, '/zh/')
  b.window.xLearningAnalytics.page('/zh')
  assert.equal(b.events('page_view').length, 1)
  b.window.xLearningAnalytics.startLearning('video')
  assert.equal(b.events('start_learning')[0].landing_page, '/zh/')
})

test('business events allow only categorical metadata and never forward arbitrary fields', () => {
  const b = browser('/dashboard/youtube')
  b.window.xLearningAnalytics.setAuthenticated(true)
  b.window.xLearningAnalytics.event('video_selected', { content_language: 'en', content_category: 'agents', entry_point: 'learning_path', title: 'private', video_id: 'private', email: 'secret@example.com' })
  const event = b.events('video_selected')[0]
  assert.equal(event.auth_state, 'logged_in')
  assert.equal(event.content_category, 'agents')
  b.window.xLearningAnalytics.event('video_start', { content_language: 'private', content_category: 'private', entry_point: 'private' })
  assert.equal(b.events('video_start')[0].content_language, 'unknown')
  b.window.xLearningAnalytics.event('arbitrary_private_event', {})
  b.window.xLearningAnalytics.event('learning_milestone', { learning_seconds: 61 })
  assert.equal(b.events('learning_milestone').length, 0)
  assert(!JSON.stringify(b.window.dataLayer).includes('private'))
  assert(!JSON.stringify(b.window.dataLayer).includes('secret'))
})

test('purchase is deduplicated across calls and reloads; Stripe sessions cannot become transaction IDs', () => {
  const storage = new Map()
  const attach = b => { b.window.localStorage = { getItem: k => storage.get(k), setItem: (k, v) => storage.set(k, v) }; return b.window.xLearningAnalytics }
  const b = browser('/dashboard/billing')
  const a = attach(b)
  const order = { transaction_id: '12345678-1234-1234-1234-123456789abc' }
  a.event('purchase', { transaction_id: 'cs_private_session' })
  a.event('purchase', order)
  a.event('purchase', order)
  assert.equal(b.events('purchase').length, 1)
  assert.equal(b.events('purchase')[0].value, 99)
  assert.equal(b.events('purchase')[0].currency, 'CNY')
  const reloaded = browser('/dashboard/billing')
  attach(reloaded).event('purchase', order)
  assert.equal(reloaded.events('purchase').length, 0)
})
