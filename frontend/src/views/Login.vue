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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login, checkAuth } from '@/utils/auth'

const router = useRouter()
const form = ref({
  username: '',
  password: ''
})
const loading = ref(false)
const alertMessage = ref('')
const alertType = ref('error')

onMounted(async () => {
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


