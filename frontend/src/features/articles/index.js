import articles from './discovery-loop.js'
export const ARTICLE_VARIANTS = articles
export function articleForPath(path) {
  return Object.values(articles).find(article => article.path === path)
}
export function articleRoutes(component) {
  return Object.entries(articles).map(([locale, article]) => ({
    path: article.path, name: `DiscoveryLoop-${locale}`, component,
    meta: { requiresAuth: false }
  }))
}
export function articleHead(article, origin) {
  const escape = value => value.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
  const url = origin + article.path
  const structured = {
    '@context': 'https://schema.org', '@type': 'Article', headline: article.title,
    description: article.description, inLanguage: article.lang,
    mainEntityOfPage: url, publisher: { '@type': 'Organization', name: 'X Learning' },
    isBasedOn: 'https://www.youtube.com/watch?v=ooGKNwJWdb8'
  }
  return `<title>${escape(article.title)}</title>
<meta name="description" content="${escape(article.description)}">
<link rel="canonical" href="${escape(url)}">
${[['en', articles['en-US'].path], ['zh-Hans', articles['zh-CN'].path], ['x-default', articles['en-US'].path]].map(([lang, path]) => `<link rel="alternate" hreflang="${lang}" href="${escape(origin + path)}">`).join('\n')}
<meta property="og:type" content="article">
<meta property="og:title" content="${escape(article.title)}">
<meta property="og:description" content="${escape(article.description)}">
<meta property="og:url" content="${escape(url)}">
<meta property="og:locale" content="${article.ogLocale}">
<meta name="twitter:card" content="summary">
<meta name="twitter:title" content="${escape(article.title)}">
<meta name="twitter:description" content="${escape(article.description)}">
<script id="article-schema" type="application/ld+json">${JSON.stringify(structured).replaceAll('<', '\\u003c')}</script>`
}
export function applyArticleMetadata(article, doc = globalThis.document) {
  if (!doc) return
  const origin = new URL(doc.querySelector('link[rel="canonical"]')?.href || doc.location.origin).origin
  doc.querySelectorAll('title, meta[name="description"], meta[property^="og:"], meta[name^="twitter:"], link[rel="canonical"], link[rel="alternate"][hreflang], script[type="application/ld+json"]').forEach(tag => tag.remove())
  doc.documentElement.lang = article.lang
  doc.head.insertAdjacentHTML('beforeend', articleHead(article, origin))
}
