import { createRouter, createWebHistory } from 'vue-router'
import { checkAuth } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Landing',
      component: () => import('@/views/Landing.vue')
    },
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
          path: 'stocks',
          name: 'Stocks',
          component: () => import('@/views/dashboard/Stocks.vue')
        },
        {
          path: 'youtube',
          name: 'YoutubeImport',
          component: () => import('@/views/dashboard/YoutubeImport.vue'),
          meta: { requiresAuth: false }
        }
      ]
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

export default router


