import assert from 'node:assert/strict'
import { readFile, access } from 'node:fs/promises'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const dist = resolve(dirname(fileURLToPath(import.meta.url)), '../dist')
const html = await readFile(resolve(dist, 'index.html'), 'utf8')
assert.match(html, /<title>[^<]+<\/title>/i, 'Built shell must have a title')
assert.match(html, /<meta\s+name="description"\s+content="[^"]+"/i, 'Built shell must have a description')
assert.doesNotMatch(html, /(?:src|href)="\/src\//, 'Unbuilt source entry found')
const assets = [...html.matchAll(/(?:src|href)="(\/assets\/[^"?#]+)(?:[?#][^"]*)?"/g)].map(match => match[1])
assert(assets.some(path => path.endsWith('.js')), 'Built shell must reference a generated JavaScript entry')
for (const path of assets) await access(resolve(dist, `.${path}`))
const home = await readFile(resolve(dist, 'home.html'), 'utf8')
const chineseHome = await readFile(resolve(dist, 'home-zh.html'), 'utf8')
assert.match(chineseHome, /<html lang="zh-CN">/, 'Chinese URL must start in Chinese without JavaScript')
assert.match(chineseHome, /<h1[^>]*>学 AI，<br>也练/, 'Chinese homepage must render original Chinese copy')
assert.match(chineseHome, /<title>X Learning - 学 AI，也练英语<\/title>/)
assert.match(chineseHome, /class="brand-logo[^>]*|brand-logo"/, 'Chinese homepage keeps the original logo')
assert(chineseHome.includes('href="/zh/"'), 'Chinese logo must keep visitors on the Chinese homepage')
for (const [document, path] of [[home, '/'], [chineseHome, '/zh/']]) {
  const canonical = document.match(/<link rel="canonical" href="([^"]+)"/)[1]
  const origin = new URL(canonical).origin
  assert.equal(canonical, origin + path)
  for (const [language, alternate] of [['en', '/'], ['zh-Hans', '/zh/'], ['x-default', '/']]) {
    assert(document.includes(`hreflang="${language}" href="${origin}${alternate}"`), 'Missing reciprocal language alternate')
  }
  for (const match of document.matchAll(/(?:src|href)="(\/assets\/[^"?#]+)(?:[?#][^"]*)?"/g)) {
    await access(resolve(dist, `.${match[1]}`))
  }
}
assert.match(home, /<html lang="en">/, 'Prerendered language must match the default English content')
assert.match(home, /data-prerendered="true"/, 'Missing prerendered homepage')
assert.match(home, /<h1[^>]*>Explore AI\.<br>Build your /, 'Homepage must include the original heading without JavaScript')
for (const path of ['/guides', '/video', '/dashboard/youtube', '/lingq-lab']) {
  assert(home.includes(`href="${path}"`), `Missing crawlable homepage link: ${path}`)
}
assert.match(home, /class="demo-tabs"/, 'Original interactive demo must remain in the rendered component')
assert.match(home, /Language \/ 语言/, 'Original language selector must remain available')
for (const match of home.matchAll(/(?:src|href)="(\/assets\/[^"?#]+)(?:[?#][^"]*)?"/g)) {
  await access(resolve(dist, `.${match[1]}`))
}
const robots = await readFile(resolve(dist, 'robots.txt'), 'utf8')
assert.match(robots, /^User-agent:\s*\*/mi, 'robots.txt must declare a crawler policy')
assert.match(robots, /^Sitemap:\s*https?:\/\/\S+\/sitemap\.xml\s*$/mi, 'robots.txt must link to the sitemap')
console.log(`Verified frontend build: prerendered content, original controls, metadata, robots.txt, and linked assets`)

// Both document entry points share exactly one analytics initializer.
await access(resolve(dist, 'js/learning-analytics.js'))
for (const [name, document] of [['index.html', html], ['home.html', home], ['home-zh.html', chineseHome]]) {
  assert.equal((document.match(/src="\/js\/learning-analytics\.js"/g) || []).length, 1, `${name} must load the shared tracker exactly once`)
  assert.doesNotMatch(document, /gtag\(['"]config['"]/, `${name} must not initialize GA again inline`)
}

// Article SEO must be present in the initial HTML, not added only by the client.
const { ARTICLE_VARIANTS } = await import('../src/features/articles/index.js')
for (const article of Object.values(ARTICLE_VARIANTS)) {
  const document = await readFile(resolve(dist, article.file), 'utf8')
  assert(document.includes(`<html lang="${article.lang}">`))
  assert.equal((document.match(/<h1[\s>]/g) || []).length, 1)
  assert(document.includes(article.title))
  assert(document.includes(article.description))
  const canonical = document.match(/<link rel="canonical" href="([^"]+)"/)[1]
  const origin = new URL(canonical).origin
  assert.equal(canonical, origin + article.path)
  assert(document.includes(`hreflang="en" href="${origin}${ARTICLE_VARIANTS['en-US'].path}"`))
  assert(document.includes(`hreflang="zh-Hans" href="${origin}${ARTICLE_VARIANTS['zh-CN'].path}"`))
  const schema = JSON.parse(document.match(/<script id="article-schema" type="application\/ld\+json">([\s\S]*?)<\/script>/)[1])
  assert.equal(schema['@type'], 'Article')
  assert.equal(schema.mainEntityOfPage, canonical)
  assert.equal(schema.inLanguage, article.lang)
  assert.equal((document.match(/application\/ld\+json/g) || []).length, 1)
  for (const section of article.sections) assert(document.includes(`id="${section[0]}"`))
  for (const [question, answer] of article.faq) {
    assert(document.includes(question)); assert(document.includes(answer))
  }
  for (const match of document.matchAll(/(?:src|href)="(\/assets\/[^"?#]+)(?:[?#][^"]*)?"/g)) await access(resolve(dist, `.${match[1]}`))
}
console.log('Verified both article documents: content, canonical, hreflang, schema, FAQ, and assets')
