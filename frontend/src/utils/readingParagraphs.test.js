import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readingParagraphs } from './readingParagraphs.js'
const sentence = (id, startTime, endTime, text) => ({ id, startTime, endTime, text })
test('splits on pauses and preserves every sentence in order', () => {
  const input = [sentence(1, 0, 2, 'Hello'), sentence(2, 2, 4, 'world.'), sentence(3, 6, 8, 'Next topic.')]
  const groups = readingParagraphs(input)
  assert.deepEqual(groups.map(g => g.sentences.map(s => s.id)), [[1, 2], [3]])
  assert.deepEqual(groups.flatMap(g => g.sentences), input)
  assert.equal(groups[0].endTime, 4)
})
test('prefers sentence endings after a readable length and caps unpunctuated blocks', () => {
  const input = [sentence(1, 0, 1, 'word '.repeat(54)), sentence(2, 1, 2, 'End.'), sentence(3, 2, 3, 'word '.repeat(110)), sentence(4, 3, 4, 'Last')]
  assert.deepEqual(readingParagraphs(input).map(g => g.sentences.length), [2, 1, 1])
  assert.deepEqual(readingParagraphs([]), [])
})
