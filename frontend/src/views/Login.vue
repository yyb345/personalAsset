<template>
  <div class="auth-page">
    <div class="login-container">
      <div class="login-brand"><BrandLogo /></div>
      <div class="login-header">
        <h1>{{ $t('auth.welcomeBack') }}</h1>
        <p>{{ $t('auth.signInContinue') }}</p>
      </div>

      <div v-if="alertMessage" class="alert alert-error" role="alert">
        {{ alertMessage }}
      </div>

      <!-- OAuth2 登录按钮 -->
      <div class="oauth-buttons">
        <a :href="googleLoginUrl" class="oauth-btn google-btn">
          <svg class="google-icon" viewBox="0 0 24 24" width="20" height="20">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          <span>{{ $t('auth.signInWithGoogle') }}</span>
        </a>
      </div>

    </div>

    <div class="back-home">
      <router-link to="/">{{ $t('auth.backToHome') }}</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { checkAuth } from '@/utils/auth'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const alertMessage = ref('')

// OAuth2 Google 登录 URL（直接指向后端）
const googleLoginUrl = computed(() => {
  // 开发环境使用后端地址，生产环境使用相对路径
  const baseUrl = import.meta.env.DEV ? 'http://localhost:8081' : ''
  return `${baseUrl}/oauth2/authorization/google`
})

onMounted(async () => {
  // 检查 URL 中是否有 OAuth2 错误参数
  if (route.query.error === 'oauth2') {
    alertMessage.value = t('auth.oauth2Failed')
  }

  // 检查是否已登录
  const isAuthenticated = await checkAuth()
  if (isAuthenticated) {
    router.replace('/dashboard')
  }
})

</script>

<style src="@/assets/styles/login.css"></style>


