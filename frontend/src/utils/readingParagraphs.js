// Use pauses and sentence endings as reading boundaries; cap long unpunctuated blocks.
export function readingParagraphs(sentences = []) {
  const groups = []
  let chunk = [], words = 0
  const flush = () => {
    if (!chunk.length) return
    groups.push({ startTime: chunk[0].startTime, endTime: chunk.at(-1).endTime, sentences: chunk })
    chunk = []; words = 0
  }
  for (const sentence of sentences) {
    const previous = chunk.at(-1)
    if (previous && sentence.startTime - previous.endTime >= 1.5) flush()
    chunk.push(sentence)
    words += sentence.text.trim().split(/\s+/).filter(Boolean).length
    if (words >= 110 || (words >= 55 && /[.!?。！？]["'”’]*$/.test(sentence.text.trim()))) flush()
  }
  flush()
  return groups
}
