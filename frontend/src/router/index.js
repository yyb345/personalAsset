import { createRouter, createWebHistory } from 'vue-router'
import { checkAuth } from '@/utils/auth'
import { setLocale } from '@/i18n'
import { localeForHomePath, applyHomeMetadata } from '@/features/homepage/metadata'
import { homeRoutes } from '@/features/homepage/routes'

import { articleRoutes, articleForPath, applyArticleMetadata } from '@/features/articles'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...articleRoutes(() => import('@/views/DiscoveryLoopArticle.vue')),
    { path: '/lingq-lab', name: 'LingqLab', component: () => import('@/views/LingqLab.vue'), meta: { requiresAuth: false } },
    { path: '/dashboard/billing', name: 'Billing', component: () => import('@/views/dashboard/Billing.vue'), meta: { requiresAuth: false } },
    ...homeRoutes(() => import('@/views/Landing.vue')),
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue')
    },
    {
      path: '/dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/dashboard/youtube'
        },
        {
          path: 'youtube',
          name: 'YoutubeImport',
          component: () => import('@/views/dashboard/YoutubeImport.vue'),
          meta: { requiresAuth: false }
        },
        {
          path: 'xiaohongshu',
          name: 'XiaohongshuImport',
          component: () => import('@/views/dashboard/XiaohongshuImport.vue'),
          meta: { requiresAuth: false }
        },
        {
          path: 'study-workspace/:videoId',
          name: 'StudyWorkspace',
          component: () => import('@/views/dashboard/StudyWorkspace.vue'),
          meta: { requiresAuth: false }
        }
      ]
    },
    {
      path: '/shadowing',
      name: 'Shadowing',
      component: () => import('@/views/Shadowing.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/transcribe',
      name: 'TranscribeRead',
      component: () => import('@/views/TranscribeRead.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 检查当前路由是否明确设置了 requiresAuth
  // 优先使用最具体的路由配置（子路由），如果子路由明确设置为 false，则不需要认证
  const currentRoute = to.matched[to.matched.length - 1]
  const explicitAuth = currentRoute?.meta?.requiresAuth
  
  // 如果明确设置为 false，则不需要认证
  if (explicitAuth === false) {
    next()
    return
  }
  
  // 否则检查是否有任何父路由要求认证
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  
  if (requiresAuth) {
    const isAuthenticated = await checkAuth()
    if (!isAuthenticated) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

// One sanitized page_view per successful navigation; the initial document is deduplicated.
router.afterEach((to, _from, failure) => {
  if (!failure) {
    const article = articleForPath(to.path)
    if (article) {
      setLocale(article.lang === 'zh-CN' ? 'zh-CN' : 'en-US')
      applyArticleMetadata(article)
    } else {
      document.querySelector('#article-schema')?.remove()
    }
    const locale = localeForHomePath(to.path)
    if (locale) { setLocale(locale); applyHomeMetadata(locale) }
    window.xLearningAnalytics?.page(to.path)
  }
})

export default router


