<template>
  <div class="auth-page">
    <div class="background-orbs">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>

    <div class="login-container">
      <router-link to="/" class="logo-link">
        <span class="logo-icon">📚</span>
        <span class="logo-text">X Learning</span>
      </router-link>
      <div class="login-header">
        <h1>Welcome Back</h1>
        <p>Sign in to continue your learning journey</p>
      </div>

      <div v-if="alertMessage" :class="['alert', `alert-${alertType}`]">
        {{ alertMessage }}
      </div>

      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">Username</label>
          <input 
            type="text" 
            id="username" 
            v-model="form.username" 
            required 
            autocomplete="username"
            placeholder="Enter your username"
          >
        </div>

        <div class="form-group">
          <label for="password">Password</label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            required 
            autocomplete="current-password"
            placeholder="Enter your password"
          >
        </div>

        <button type="submit" class="btn-login" :disabled="loading">
          {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>

      <!-- OAuth2 分隔线 -->
      <div class="oauth-divider">
        <span>or continue with</span>
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
          <span>Sign in with Google</span>
        </a>
      </div>

      <div class="login-footer">
        Don't have an account? <router-link to="/register">Sign Up</router-link>
      </div>
    </div>

    <div class="back-home">
      <router-link to="/">← Back to Home</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, checkAuth } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const form = ref({
  username: '',
  password: ''
})
const loading = ref(false)
const alertMessage = ref('')
const alertType = ref('error')

// OAuth2 Google 登录 URL（直接指向后端）
const googleLoginUrl = computed(() => {
  // 开发环境使用后端地址，生产环境使用相对路径
  const baseUrl = import.meta.env.DEV ? 'http://localhost:8081' : ''
  return `${baseUrl}/oauth2/authorization/google`
})

onMounted(async () => {
  // 检查 URL 中是否有 OAuth2 错误参数
  if (route.query.error === 'oauth2') {
    showAlert('OAuth2 login failed. Please try again or use username/password.')
  }

  // 检查是否已登录
  const isAuthenticated = await checkAuth()
  if (isAuthenticated) {
    router.push('/dashboard')
  }
})

const showAlert = (message, type = 'error') => {
  alertMessage.value = message
  alertType.value = type
  if (type !== 'success') {
    setTimeout(() => {
      alertMessage.value = ''
    }, 5000)
  }
}

const handleLogin = async () => {
  loading.value = true
  try {
    await login(form.value.username, form.value.password)
    showAlert('Login successful! Redirecting...', 'success')
    setTimeout(() => {
      router.push('/dashboard')
    }, 1000)
  } catch (error) {
    showAlert(error.response?.data?.message || 'Login failed. Please try again.')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped src="@/assets/styles/auth.css"></style>


