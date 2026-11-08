import assert from 'node:assert/strict'
import { readFile, writeFile, rm } from 'node:fs/promises'
import { resolve, dirname } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { ARTICLE_VARIANTS, articleHead } from '../src/features/articles/index.js'
import { HOME_VARIANTS } from '../src/features/homepage/metadata.js'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const dist = resolve(root, 'dist')
const { render } = await import(pathToFileURL(resolve(root, '.prerender/prerender-entry.js')).href)
const manifest = JSON.parse(await readFile(resolve(dist, '.vite/manifest.json'), 'utf8'))
const css = new Set()
const visited = new Set()
function collect(key) {
  if (visited.has(key)) return
  visited.add(key)
  const chunk = manifest[key]
  assert(chunk, `Missing build manifest entry: ${key}`)
  for (const dependency of chunk.imports || []) collect(dependency)
  for (const file of chunk.css || []) css.add(file)
}
collect('src/views/Landing.vue')
const shell = await readFile(resolve(dist, 'index.html'), 'utf8')
const extraStyles = [...css].filter(file => !shell.includes(`href="/${file}"`))
const origin = new URL(shell.match(/<link rel="canonical" href="([^"]+)"/)[1]).origin
for (const [locale, variant] of Object.entries(HOME_VARIANTS)) {
  const body = await render(locale)
  assert.match(body, /<h1[\s>]/, 'The original landing component must render its main heading')
  const alternates = [
    ['en', '/'], ['zh-Hans', '/zh/'], ['x-default', '/']
  ].map(([language, path]) => `<link rel="alternate" hreflang="${language}" href="${origin}${path}">`).join('\n')
  const html = shell
    .replace(/(<html\b[^>]*lang=")[^"]*(")/, `$1${variant.lang}$2`)
    .replace(/<title>[^<]*<\/title>/, `<title>${variant.title}</title>`)
    .replace(/(<meta name="description" content=")[^"]*(")/, `$1${variant.description}$2`)
    .replace(/(<link rel="canonical" href=")[^"]*(")/, `$1${origin}${variant.path}$2`)
    .replace(/(<meta property="og:url" content=")[^"]*(")/, `$1${origin}${variant.path}$2`)
    .replace(/(<meta (?:property="og:title"|name="twitter:title") content=")[^"]*(")/g, `$1${variant.title}$2`)
    .replace(/(<meta (?:property="og:description"|name="twitter:description") content=")[^"]*(")/g, `$1${variant.description}$2`)
    .replace('</head>', extraStyles.map(file => `<link rel="stylesheet" href="/${file}">`).join('\n')
      + `\n<meta property="og:locale" content="${variant.ogLocale}">\n${alternates}\n</head>`)
    .replace('<div id="app"></div>', () => `<div id="app" data-prerendered="true">${body}</div>`)
  assert(html.includes('data-prerendered="true"'), 'Failed to insert rendered homepage')
  await writeFile(resolve(dist, variant.file), html)
  console.log(`Prerendered original landing page: ${variant.path} (${locale})`)
}
// Article documents include the full body and reciprocal language metadata before JavaScript runs.
collect('src/views/DiscoveryLoopArticle.vue')
for (const [locale, article] of Object.entries(ARTICLE_VARIANTS)) {
  const body = await render(locale, article.path)
  assert(body.includes(article.title), 'Article body must contain its heading')
  const html = shell
    .replace(/(<html\b[^>]*lang=")[^"]*(")/, `$1${article.lang}$2`)
    .replace(/<title>[\s\S]*?<\/title>/g, '')
    .replace(/<meta (?:name="(?:description|twitter:[^"]+)"|property="og:[^"]+")[^>]*>/g, '')
    .replace(/<link rel="(?:canonical|alternate)"[^>]*>/g, '')
    .replace(/<script type="application\/ld\+json">[\s\S]*?<\/script>/g, '')
    .replace('</head>', [...css].filter(file => !shell.includes(`href="/${file}"`)).map(file => `<link rel="stylesheet" href="/${file}">`).join('\n') + articleHead(article, origin) + '</head>')
    .replace('<div id="app"></div>', () => `<div id="app" data-prerendered="true">${body}</div>`)
  await writeFile(resolve(dist, article.file), html)
  console.log(`Prerendered article: ${article.path}`)
}
await rm(resolve(root, '.prerender'), { recursive: true, force: true })
