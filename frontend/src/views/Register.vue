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
        <span class="logo-text">English Evolution</span>
      </router-link>
      <div class="login-header">
        <h1>Create Account</h1>
        <p>Start your English learning adventure</p>
      </div>

      <div v-if="alertMessage" :class="['alert', `alert-${alertType}`]">
        {{ alertMessage }}
      </div>

      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">Username</label>
          <input 
            type="text" 
            id="username" 
            v-model="form.username" 
            required 
            autocomplete="username"
            minlength="3"
            placeholder="At least 3 characters"
          >
        </div>

        <div class="form-group">
          <label for="fullName">Full Name</label>
          <input 
            type="text" 
            id="fullName" 
            v-model="form.fullName" 
            required
            placeholder="Enter your full name"
          >
        </div>

        <div class="form-group">
          <label for="email">Email</label>
          <input 
            type="email" 
            id="email" 
            v-model="form.email" 
            required
            placeholder="example@email.com"
          >
        </div>

        <div class="form-group">
          <label for="password">Password</label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            required 
            autocomplete="new-password"
            minlength="6"
            placeholder="At least 6 characters"
          >
        </div>

        <div class="form-group">
          <label for="confirmPassword">Confirm Password</label>
          <input 
            type="password" 
            id="confirmPassword" 
            v-model="form.confirmPassword" 
            required 
            autocomplete="new-password"
            placeholder="Re-enter your password"
          >
        </div>

        <button type="submit" class="btn-login" :disabled="loading">
          {{ loading ? 'Signing up...' : 'Sign Up' }}
        </button>
      </form>

      <div class="login-footer">
        Already have an account? <router-link to="/login">Sign In</router-link>
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
import { register, checkAuth } from '@/utils/auth'

const router = useRouter()
const form = ref({
  username: '',
  fullName: '',
  email: '',
  password: '',
  confirmPassword: ''
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

const handleRegister = async () => {
  if (form.value.password !== form.value.confirmPassword) {
    showAlert('Passwords do not match')
    return
  }

  loading.value = true
  try {
    await register({
      username: form.value.username,
      fullName: form.value.fullName,
      email: form.value.email,
      password: form.value.password
    })
    showAlert('Registration successful! Redirecting to login...', 'success')
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (error) {
    showAlert(error.response?.data?.message || 'Registration failed. Please try again.')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped src="@/assets/styles/auth.css"></style>


