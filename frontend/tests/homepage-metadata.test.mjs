import test from 'node:test'
import assert from 'node:assert/strict'
import { HOME_VARIANTS, localeForHomePath, applyHomeMetadata } from '../src/features/homepage/metadata.js'
import { homeRoutes } from '../src/features/homepage/routes.js'
import { createRouter, createMemoryHistory } from 'vue-router'

function documentFixture() {
  const elements = []
  function element(name) {
    const attributes = {}
    const tag = { name, attributes, setAttribute(key, value) { attributes[key] = value }, remove() { elements.splice(elements.indexOf(tag), 1) } }
    for (const key of ['href', 'rel', 'hreflang']) Object.defineProperty(tag, key, {
      get: () => attributes[key], set: value => { attributes[key] = value }
    })
    return tag
  }
  function matches(tag, selector) {
    return tag.name === selector.match(/^\w+/)[0] && [...selector.matchAll(/\[([\w-]+)(?:="([^"]*)")?\]/g)]
      .every(([, key, value]) => value === undefined ? key in tag.attributes : tag.attributes[key] === value)
  }
  const doc = {
    documentElement: {}, location: { origin: 'http://localhost:8082' },
    head: { appendChild: tag => elements.push(tag) }, createElement: element,
    querySelector: selector => elements.find(tag => matches(tag, selector)),
    querySelectorAll: selector => elements.filter(tag => matches(tag, selector))
  }
  const canonical = element('link'); canonical.rel = 'canonical'; canonical.href = 'https://learn.example/'
  elements.push(canonical)
  return doc
}

test('explicit homepage URLs select a stable language and leave workspace preferences independent', () => {
  assert.equal(localeForHomePath('/zh/'), 'zh-CN')
  assert.equal(localeForHomePath('/zh'), 'zh-CN')
  assert.equal(localeForHomePath('/'), 'en-US')
  assert.equal(localeForHomePath('/index.html'), 'en-US')
  assert.equal(localeForHomePath('/dashboard/youtube'), null)
})

test('language switch navigates between distinct routes rather than being rejected as an alias duplicate', async () => {
  const router = createRouter({ history: createMemoryHistory(), routes: homeRoutes({ render: () => null }) })
  await router.push('/zh/')
  await router.isReady()
  assert.equal(await router.push('/'), undefined)
  assert.equal(router.currentRoute.value.path, '/')
  assert.equal(await router.push('/zh/'), undefined)
  assert.equal(router.currentRoute.value.path, '/zh/')
})

test('switching homepage language updates self canonical and reciprocal alternates without duplicates', () => {
  const doc = documentFixture()
  for (const locale of ['zh-CN', 'en-US', 'zh-CN']) {
    applyHomeMetadata(locale, doc)
    const variant = HOME_VARIANTS[locale]
    assert.equal(doc.documentElement.lang, variant.lang)
    assert.equal(doc.title, variant.title)
    assert.equal(doc.querySelector('link[rel="canonical"]').href, 'https://learn.example' + variant.path)
    assert.equal(doc.querySelector('meta[name="description"]').attributes.content, variant.description)
    assert.equal(doc.querySelector('meta[property="og:url"]').attributes.content, 'https://learn.example' + variant.path)
    assert.equal(doc.querySelectorAll('link[rel="alternate"][hreflang]').length, 3)
    assert.equal(doc.querySelector('link[hreflang="zh-Hans"]').href, 'https://learn.example/zh/')
    assert.equal(doc.querySelector('link[hreflang="en"]').href, 'https://learn.example/')
    assert.equal(doc.querySelector('link[hreflang="x-default"]').href, 'https://learn.example/')
    assert.equal(doc.querySelectorAll('meta[name="description"]').length, 1)
  }
})
