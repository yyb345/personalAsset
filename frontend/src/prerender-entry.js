import { createSSRApp } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { createRouter, createMemoryHistory } from 'vue-router'
import App from './App.vue'
import Landing from './views/Landing.vue'
import i18n from './i18n'

// Render the same components and translations as the client, without browser globals or API calls.
export async function render() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: Landing },
      // Resolve links using normal router semantics without loading workspace code.
      { path: '/:pathMatch(.*)*', component: { render: () => null } }
    ]
  })
  const app = createSSRApp(App)
  app.use(router)
  app.use(i18n)
  await router.push('/')
  await router.isReady()
  return renderToString(app)
}
