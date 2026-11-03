// Local UI replay of real production subtitles and actual Java highlight API results.
// First run: JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -Dtest=OnlineSubtitleMockTest test
import http from 'node:http'
import { readFile } from 'node:fs/promises'
const report = JSON.parse(await readFile(new URL('../../target/glossary-online-mock.json', import.meta.url), 'utf8'))
const results = new Map(report.highlight.results.map(result => [result.text, result]))
const port = Number(process.env.GLOSSARY_MOCK_PORT || 18081)
let failNext = process.env.GLOSSARY_MOCK_FAIL_FIRST === 'true'
const server = http.createServer(async (req, res) => {
  const origin = req.headers.origin || ''
  if (/^http:\/\/(localhost|127\.0\.0\.1):(3000|3001|3100)$/.test(origin)) res.setHeader('Access-Control-Allow-Origin', origin)
  res.setHeader('Access-Control-Allow-Credentials', 'true')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type')
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS')
  res.setHeader('Content-Type', 'application/json; charset=utf-8')
  const send = (code, data) => { res.writeHead(code); res.end(JSON.stringify(data)) }
  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return }
  const path = new URL(req.url, 'http://localhost').pathname
  if (req.method === 'GET' && [`/api/youtube/status/${report.video.videoId}`, `/api/youtube/sentences/${report.video.videoId}`].includes(path)) {
    send(200, report.video); return
  }
  if (req.method === 'GET' && path === '/__mock/stats') { send(200, report.stats); return }
  if (req.method === 'POST' && path === '/api/glossary/highlight') {
    if (failNext) { failNext = false; req.resume(); send(503, { error: 'Intentional mock failure; retry should recover' }); return }
    try {
      let body = ''
      for await (const chunk of req) {
        body += chunk
        if (body.length > 1_000_000) { send(413, { error: 'Payload too large' }); return }
      }
      const { texts } = JSON.parse(body)
      if (!Array.isArray(texts) || texts.length < 1 || texts.length > 100 || texts.some(t => !results.has(t))) {
        send(400, { error: 'Replay accepts only captured transcript sentences' }); return
      }
      console.log(`highlight batch=${texts.length}`)
      send(200, { engine: 'local', results: texts.map(text => results.get(text)) }); return
    } catch { send(400, { error: 'Invalid mock request' }); return }
  }
  send(404, { error: 'Not part of subtitle mock', path })
})
server.listen(port, '127.0.0.1', () => console.log(`Subtitle mock API http://localhost:${port} — ${JSON.stringify(report.stats)}`))
