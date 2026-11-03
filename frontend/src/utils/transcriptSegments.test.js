import { test } from 'node:test'
import assert from 'node:assert/strict'
import { transcriptSegments } from './transcriptSegments.js'

test('preserves text and UTF-16 term offsets, including HTML-shaped text', () => {
  const text = '😀 LLM <script>alert(1)</script> 词元'
  const segments = transcriptSegments(text, [{ start: 3, end: 6, termEn: 'LLM' }])
  assert.equal(segments.map(part => part.text).join(''), text)
  assert.equal(segments.find(part => part.term).text, 'LLM')
})
test('keeps manual highlights overlapping glossary terms and repeated marks', () => {
  const segments = transcriptSegments('LLM and LLM', [{ start: 0, end: 3 }], [{ id: 2, text: 'LLM' }])
  assert.equal(segments.filter(part => part.highlight).length, 2)
  assert.equal(segments.find(part => part.term).highlight.id, 2)
})
test('rejects invalid offsets and prefers longest terms at the same position', () => {
  const segments = transcriptSegments('large language model', [
    { start: -1, end: 5 }, { start: 0, end: 100 }, { start: 0, end: 5 }, { start: 0, end: 20 }, { start: 6, end: 14 }
  ])
  assert.equal(segments.length, 1)
  assert.equal(segments[0].term.end, 20)
  assert.deepEqual(transcriptSegments(''), [])
})
