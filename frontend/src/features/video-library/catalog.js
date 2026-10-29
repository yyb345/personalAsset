import { productionEditorial, curatedSequences } from './production-editorial.js'

// Keep editorial overrides separate from inference. Add reviewed videos by YouTube ID.
export const videoEditorial = {
  ...productionEditorial,
  Pz68t3RGeqI: { category: 'english', objective: { 'zh-CN': '积累描述忙碌一天的日常英语表达。', 'en-US': 'Build everyday expressions for describing a busy day.' } },
  nh36jE2M064: { category: 'english', objective: { 'zh-CN': '学习描述晨间习惯的词汇与表达。', 'en-US': 'Learn vocabulary and expressions for a morning routine.' } }
}

export const categories = ['all', 'basics', 'models', 'agents', 'applications', 'english', 'business', 'technology', 'japanese', 'other']
export const learningPaths = [
  { id: 'foundations', stages: ['basics', 'models'], goal: 'foundations' },
  { id: 'building', stages: ['agents', 'applications'], goal: 'building' },
  { id: 'english', stages: ['english'], goal: 'english' }
]

const categoryRules = [
  ['agents', /\b(agents?|agentic|langgraph|autogen|crewai|mcp)\b|智能体/i],
  ['applications', /\b(rag|workflow|automation|n8n|comfyui|prompt(?:ing)?)\b|工作流|提示词|应用实战/i],
  ['models', /\b(llms?|gpt\w*|claude|gemini|transformers?|diffusion|fine.?tun\w*|large language models?)\b|大模型|微调/i],
  ['basics', /\b(ai|artificial intelligence|machine learning|deep learning|neural networks?)\b|人工智能|机器学习|深度学习|神经网络/i],
  ['english', /\b(english|vlog|listening|pronunciation|shadowing)\b|英语|口语|听力/i]
]
const difficultyRank = { easy: 0, medium: 1, hard: 2, unknown: 3 }

export function enrichVideo(video) {
  const editorial = videoEditorial[video.videoId] || {}
  const category = editorial.category || categoryRules.find(([, pattern]) => pattern.test(video.title || ''))?.[0] || 'other'
  const difficulty = editorial.difficulty || video.difficultyLevel
  return {
    ...video,
    category,
    difficulty: ['easy', 'medium', 'hard'].includes(difficulty) ? difficulty : 'unknown',
    objective: editorial.objective || null,
    editorial: Boolean(editorial.category),
    // Preview eligibility belongs to the original catalog, never the filtered list.
    previewAllowed: Boolean(video.previewAllowed)
  }
}

export function filterVideos(videos, filters) {
  const path = learningPaths.find(item => item.id === filters.path)
  const sequence = path ? curatedSequences[path.id] : null
  const matches = videos.filter(video => {
    const duration = Number(video.duration) || 0
    return (filters.category === 'all' || video.category === filters.category)
      && (!path || (sequence.includes(video.videoId) && video.status === 'completed'))
      && (filters.difficulty === 'all' || video.difficulty === filters.difficulty)
      && (filters.duration === 'all'
        || (duration > 0 && (filters.duration === 'short' ? duration <= 600
          : filters.duration === 'medium' ? duration > 600 && duration <= 1200 : duration > 1200)))
  })
  if (path) {
    return [...matches].sort((a, b) => sequence.indexOf(a.videoId) - sequence.indexOf(b.videoId)
      || difficultyRank[a.difficulty] - difficultyRank[b.difficulty]
      || (a.duration || Infinity) - (b.duration || Infinity)
      || String(a.videoId).localeCompare(String(b.videoId)))
  }
  return matches
}
