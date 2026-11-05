export const STORAGE_KEY = 'xlearning.lingq-lab.v1'
export const lessons = [
  { id: 'ai', title: 'A small guide to a big idea', subtitle: 'Why artificial intelligence learns from examples', topic: 'Artificial intelligence', level: 'B1', icon: '✦', color: 'mint', minutes: 3, text: 'Artificial intelligence learns from examples. A model discovers patterns in data and uses these patterns to make predictions. The process is powerful, but it is not magic.\n\nImagine teaching a machine to recognize a tree. You share many examples of trees in different places. Over time, the model learns which features matter. Good data makes a real difference.\n\nA useful question is not whether a machine can think. Ask what the model can help people understand, and where human judgment is still essential.' },
  { id: 'design', title: 'Technology that gives us time', subtitle: 'A calmer way to think about useful tools', topic: 'Design & technology', level: 'B1', icon: '◷', color: 'peach', minutes: 3, text: 'Good technology gives people time to think. A useful tool makes a difficult process feel simple. It helps us understand what matters and leaves space for human judgment.\n\nImagine a quiet morning without constant notifications. You choose one meaningful task and give it your attention. Small changes in design can make a real difference.\n\nThe next great product may not need more features. It may simply help people focus on the world around them.' },
  { id: 'space', title: 'The long road to another world', subtitle: 'What exploring space teaches us about Earth', topic: 'Science', level: 'B2', icon: '↗', color: 'lavender', minutes: 4, text: 'Exploring space begins with a question. What can a distant world teach us about our own planet? Scientists build tools to collect data and discover patterns that human eyes cannot see.\n\nA mission requires patience. Every prediction must survive careful testing. When a machine travels beyond Earth, small decisions can make a real difference.\n\nScience is a shared process. Each new discovery gives people another reason to stay curious.' }
]
export const dictionary = { artificial: '人工的；人造的', intelligence: '智能；理解力', learns: '学习；学会', examples: '例子；示例', model: '模型', discovers: '发现', patterns: '规律；模式', data: '数据', predictions: '预测', process: '过程；流程', powerful: '强大的', imagine: '想象', machine: '机器', recognize: '识别；认出', features: '特征；功能', judgment: '判断力', essential: '必不可少的', technology: '技术；科技', useful: '有用的', human: '人类的', meaningful: '有意义的', attention: '注意力', design: '设计', exploring: '探索', space: '太空；空间', scientists: '科学家', discovery: '发现', curious: '好奇的', difference: '差别；影响', think: '思考', people: '人们', world: '世界', time: '时间', question: '问题', understand: '理解', tools: '工具', simple: '简单的', science: '科学', learning: '学习', good: '好的', tree: '树', trees: '树木' }
export function normalize(word) { return word.toLowerCase().replace(/[^a-z'-]/g, '') }
export const statusLabels = { 1: '1 · 新词', 2: '2 · 记不住', 3: '3 · 不确定', 4: '4 · 已学会', known: '已掌握', ignored: '已忽略' }
export function isLearning(entry) { return entry && ['1','2','3','4'].includes(String(entry.status)) }
export function newWordPercent(text, vocabulary) {
  const words = [...new Set((text.match(/[A-Za-z]+(?:['’-][A-Za-z]+)*/g) || []).map(normalize))]
  return words.length ? Math.round(words.filter(word => !vocabulary[word]).length / words.length * 100) : 0
}
export function readState(storage = localStorage) {
  const defaults = { vocabulary: {}, imports: [], completed: [], reviewCount: 0, lastLesson: 'ai', reader: { size: 22, theme: 'paper', font: 'serif' }, sidebarCollapsed: false, playlists: [] }
  try {
    const saved = JSON.parse(storage.getItem(STORAGE_KEY) || '{}') || {}
    const vocabulary = {}
    if (saved.vocabulary && typeof saved.vocabulary === 'object') for (const [word, entry] of Object.entries(saved.vocabulary)) {
      if (!entry || typeof entry !== 'object') continue
      const status = entry.status === 'learning' ? '1' : String(entry.status)
      if (!statusLabels[status]) continue
      vocabulary[word] = { ...entry, status, dueAt: Number(entry.dueAt) || 0 }
    }
    return { ...defaults, vocabulary, imports: Array.isArray(saved.imports) ? saved.imports.filter(l => l && typeof l.text === 'string' && typeof l.title === 'string') : [], completed: Array.isArray(saved.completed) ? saved.completed : [], reviewCount: Math.max(0, Number(saved.reviewCount) || 0), lastLesson: saved.lastLesson || 'ai', reader: { size: Math.min(32, Math.max(16, Number(saved.reader?.size) || 22)), theme: ['paper','sepia','night'].includes(saved.reader?.theme) ? saved.reader.theme : 'paper', font: saved.reader?.font === 'sans' ? 'sans' : 'serif' }, sidebarCollapsed: !!saved.sidebarCollapsed, playlists: sanitizePlaylists(saved.playlists) }
  } catch { return defaults }
}

export function scheduleReview(entry, remember, now = Date.now()) {
  const next = remember ? Number(entry.status) + 1 : 2
  return { ...entry, status: remember && next > 4 ? 'known' : String(next), dueAt: now + (remember ? Math.pow(2, next - 1) * 86400000 : 10 * 60000) }
}

export function sanitizePlaylists(value) {
  if (!Array.isArray(value)) return []
  return value.filter(p => p && typeof p.id === 'string' && typeof p.title === 'string').map(p => ({ id: p.id, title: p.title.trim() || '未命名学习列表', lessonIds: [...new Set(Array.isArray(p.lessonIds) ? p.lessonIds.filter(id => typeof id === 'string') : [])] }))
}
export function collectionStats(items, vocabulary, completed = []) {
  const tokens = items.flatMap(item => (item.text.match(/[A-Za-z]+(?:['’-][A-Za-z]+)*/g) || []).map(normalize))
  const words = [...new Set(tokens)]
  const counts = { new: 0, learning: 0, known: 0, ignored: 0 }
  for (const word of words) {
    const entry = vocabulary[word]
    counts[!entry ? 'new' : isLearning(entry) ? 'learning' : entry.status === 'known' ? 'known' : 'ignored']++
  }
  return { total: tokens.length, unique: words.length, completed: items.filter(item => completed.includes(item.id)).length, lessonCount: items.length, ...counts }
}
export function movePlaylistLesson(ids, index, delta) {
  const next = [...ids], target = index + delta
  if (index < 0 || index >= next.length || target < 0 || target >= next.length) return next
  ;[next[index], next[target]] = [next[target], next[index]]
  return next
}
