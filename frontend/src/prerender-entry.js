import { createSSRApp } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { createRouter, createMemoryHistory } from 'vue-router'
import App from './App.vue'
import Landing from './views/Landing.vue'
import { createSiteI18n } from './i18n'
import { homeRoutes } from './features/homepage/routes'

// Render the same components and translations as the client, without browser globals or API calls.
export async function render(locale = 'en-US') {
  const path = locale === 'zh-CN' ? '/zh/' : '/'
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      ...homeRoutes(Landing),
      // Resolve links using normal router semantics without loading workspace code.
      { path: '/:pathMatch(.*)*', component: { render: () => null } }
    ]
  })
  const app = createSSRApp(App)
  app.use(router)
  app.use(createSiteI18n(locale))
  await router.push(path)
  await router.isReady()
  return renderToString(app)
}
