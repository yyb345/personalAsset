export const HOME_VARIANTS = {
  'en-US': {
    path: '/', lang: 'en', hreflang: 'en', ogLocale: 'en_US', file: 'home.html',
    title: 'X Learning - Explore AI. Build your English.',
    description: 'Explore AI with English videos, transcripts, and study notes. Build your English through reading, word lookup, and vocabulary practice with X Learning.'
  },
  'zh-CN': {
    path: '/zh/', lang: 'zh-CN', hreflang: 'zh-Hans', ogLocale: 'zh_CN', file: 'home-zh.html',
    title: 'X Learning - 学 AI，也练英语',
    description: '用英文视频学习 AI，通过字幕、跟读和学习笔记理解人工智能；导入英文文章、点击查词、保存生词并用词卡复习，在真实内容中提升英语。'
  }
}

export function localeForHomePath(path) {
  if (path === '/zh' || path === '/zh/') return 'zh-CN'
  if (path === '/' || path === '/index.html') return 'en-US'
  return null
}

// Keep head metadata aligned when Vue changes language without a document reload.
export function applyHomeMetadata(locale, doc = globalThis.document) {
  if (!doc) return
  const variant = HOME_VARIANTS[locale]
  if (!variant) return
  const canonical = doc.querySelector('link[rel="canonical"]')
  const origin = new URL(canonical?.href || doc.location.origin).origin
  function meta(attribute, name, content) {
    let tag = doc.querySelector(`meta[${attribute}="${name}"]`)
    if (!tag) { tag = doc.createElement('meta'); tag.setAttribute(attribute, name); doc.head.appendChild(tag) }
    tag.setAttribute('content', content)
  }
  doc.documentElement.lang = variant.lang
  doc.title = variant.title
  meta('name', 'description', variant.description)
  for (const prefix of ['og', 'twitter']) {
    const attribute = prefix === 'og' ? 'property' : 'name'
    meta(attribute, `${prefix}:title`, variant.title)
    meta(attribute, `${prefix}:description`, variant.description)
  }
  meta('property', 'og:type', 'website')
  meta('property', 'og:url', origin + variant.path)
  meta('property', 'og:locale', variant.ogLocale)
  if (canonical) canonical.href = origin + variant.path
  else {
    const tag = doc.createElement('link'); tag.rel = 'canonical'; tag.href = origin + variant.path; doc.head.appendChild(tag)
  }
  doc.querySelectorAll('link[rel="alternate"][hreflang]').forEach(tag => tag.remove())
  for (const [language, path] of [['en', '/'], ['zh-Hans', '/zh/'], ['x-default', '/']]) {
    const tag = doc.createElement('link')
    tag.rel = 'alternate'; tag.hreflang = language; tag.href = origin + path; doc.head.appendChild(tag)
  }
}
