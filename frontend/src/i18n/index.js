import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'

// Restore an explicit language preference; new visitors start in English.
let savedLocale = 'en-US'
try {
  const stored = localStorage.getItem('locale')
  if (['en-US', 'zh-CN'].includes(stored)) savedLocale = stored
} catch { /* Language selection still works when storage is unavailable. */ }
if (typeof document !== 'undefined') document.documentElement.setAttribute('lang', savedLocale)

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: savedLocale,
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

// 切换语言并保存到 localStorage
export function setLocale(locale) {
  if (!['en-US', 'zh-CN'].includes(locale)) return
  i18n.global.locale.value = locale
  try { localStorage.setItem('locale', locale) } catch { /* Keep the in-memory preference. */ }
  document.documentElement.setAttribute('lang', locale)
}

export function getLocale() {
  return i18n.global.locale.value
}

export default i18n
