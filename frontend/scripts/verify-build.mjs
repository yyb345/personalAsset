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
