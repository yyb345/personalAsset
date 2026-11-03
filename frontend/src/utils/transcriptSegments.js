// All offsets are JavaScript UTF-16 indices, matching String.slice and the API.
export function transcriptSegments(text, spans = [], highlights = []) {
  text = String(text ?? '')
  const terms = spans.filter(s => Number.isInteger(s.start) && Number.isInteger(s.end) && s.start >= 0 && s.end > s.start && s.end <= text.length)
    .sort((a, b) => a.start - b.start || b.end - a.end)
    .filter((s, i, list) => !list.slice(0, i).some(other => other.end > s.start))
  const marks = []
  for (const highlight of highlights) {
    if (typeof highlight.text !== 'string' || !highlight.text) continue
    const haystack = text.toLowerCase()
    const needle = highlight.text.toLowerCase()
    let start = haystack.indexOf(needle)
    while (start >= 0) {
      marks.push({ start, end: start + highlight.text.length, highlight })
      start = haystack.indexOf(needle, start + needle.length)
    }
  }
  const boundaries = [...new Set([0, text.length, ...terms.flatMap(s => [s.start, s.end]), ...marks.flatMap(s => [s.start, s.end])])].sort((a, b) => a - b)
  return boundaries.slice(0, -1).map((start, i) => ({
    start,
    text: text.slice(start, boundaries[i + 1]),
    term: terms.find(s => s.start <= start && s.end > start),
    highlight: marks.find(s => s.start <= start && s.end > start)?.highlight
  }))
}
