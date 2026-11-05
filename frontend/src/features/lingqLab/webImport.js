export function safeSourceUrl(value) {
  if (typeof value !== 'string') return ''
  try {
    const url = new URL(value.trim())
    return ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password ? url.href : ''
  } catch { return '' }
}

export async function extractWebArticle(url, { signal, fetcher = fetch } = {}) {
  const safeUrl = safeSourceUrl(url)
  if (!safeUrl) throw new Error('请输入有效的 http:// 或 https:// 网页链接，不要包含账号密码。')
  const response = await fetcher('/api/learning/web-import', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ url: safeUrl }), signal
  })
  let result
  try { result = await response.json() } catch { throw new Error('服务返回了无效响应，请稍后重试。') }
  if (!response.ok) throw new Error(typeof result?.error === 'string' ? result.error : '网页提取失败，请检查链接或改用粘贴文本。')
  if (typeof result?.text !== 'string' || !result.text.trim()) throw new Error('没有提取到可阅读的正文，请改用粘贴文本。')
  if (result.text.length > 50000) throw new Error('正文超过 50,000 字符，请改用粘贴文本并缩短内容。')
  const sourceUrl = safeSourceUrl(result.sourceUrl || safeUrl)
  if (!sourceUrl) throw new Error('服务返回的来源链接无效，请重试。')
  return { title: (typeof result.title === 'string' ? result.title.trim() : '') .slice(0, 160) || new URL(sourceUrl).hostname, text: result.text.trim(), sourceUrl }
}
