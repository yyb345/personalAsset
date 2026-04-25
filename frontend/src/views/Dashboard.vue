<template>
  <div class="app-container">
    <!-- 移动端菜单按钮 -->
    <button class="mobile-menu-toggle" @click="toggleMobileSidebar">☰</button>

    <!-- 语言切换 - 右上角 -->
    <div class="language-switcher">
      <select v-model="currentLocale" @change="changeLanguage" class="language-select">
        <option value="zh-CN">中文</option>
        <option value="en-US">English</option>
      </select>
    </div>

    <!-- 侧边栏遮罩层 -->
    <div
      class="sidebar-overlay"
      :class="{ active: sidebarOpen }"
      @click="closeMobileSidebar"
    ></div>

    <aside class="sidebar" :class="{ active: sidebarOpen }">
      <!-- 用户信息区域 - 小红书风格 -->
      <div class="user-profile-card">
        <div class="profile-header">
          <div class="profile-avatar-wrapper">
            <div class="profile-avatar">
              <img v-if="userAvatar" :src="userAvatar" alt="Avatar" />
              <span v-else class="avatar-placeholder">{{ getAvatarText() }}</span>
            </div>
          </div>
          <div class="profile-info">
            <h2 class="profile-name">{{ userName || $t('common.guestUser') }}</h2>
            <div class="profile-id">ID: {{ userId || 'guest_' + Date.now() }}</div>
          </div>
        </div>

        <div class="profile-bio" v-if="userBio">
          <p>{{ userBio }}</p>
        </div>

        <div class="profile-stats">
          <div class="stat-item">
            <div class="stat-info">
              <div class="stat-label">{{ $t('dashboard.studyDays') }}</div>
              <div class="stat-value">{{ studyDays }}</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-info">
              <div class="stat-label">{{ $t('dashboard.library') }}</div>
              <div class="stat-value">{{ completedVideos }}</div>
            </div>
          </div>
        </div>

        <div class="profile-tags">
          <span class="tag">{{ userGender || $t('dashboard.profile.gender') }}</span>
          <span class="tag">{{ userLocation || $t('dashboard.profile.location') }}</span>
          <span class="tag">{{ userLevel || $t('dashboard.profile.level') }}</span>
        </div>

        <div class="profile-actions">
          <button v-if="isAuthenticated" class="btn-profile-action btn-logout" @click="handleLogout">
            <span>🚪</span> {{ $t('common.logout') }}
          </button>
          <button v-else class="btn-profile-action btn-login" @click="handleLogin">
            <span>👋</span> {{ $t('common.login') }}
          </button>
        </div>
      </div>

      <nav class="menu">
        <ul>
          <li>
            <router-link
              to="/dashboard/youtube"
              class="menu-item"
              @click="closeMobileSidebar"
            >
              <span class="menu-text">{{ $t('dashboard.youtubeVideos') }}</span>
            </router-link>
          </li>
<!--          <li>-->
<!--            <router-link-->
<!--              to="/dashboard/xiaohongshu"-->
<!--              class="menu-item"-->
<!--              @click="closeMobileSidebar"-->
<!--            >-->
<!--              <span class="menu-text">{{ $t('dashboard.xiaohongshuVideos') }}</span>-->
<!--            </router-link>-->
<!--          </li>-->
        </ul>
      </nav>
    </aside>

    <!-- 可拖拽的分隔线 -->
    <div 
      class="resizer" 
      @mousedown="startResize"
      @touchstart="startResize"
    >
      <div class="resizer-line"></div>
    </div>

    <main class="main-content" ref="mainContent">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { logout } from '@/utils/auth'
import { setLocale, getLocale } from '@/i18n'
import axios from '@/utils/axios'

const { t } = useI18n()
const router = useRouter()
const userName = ref('')
const userId = ref('')
const userAvatar = ref('')
const userBio = ref('')
const userGender = ref('')
const userLocation = ref('')
const userLevel = ref('')
const studyDays = ref(0)
const completedVideos = ref(0)
const isAuthenticated = ref(false)
const sidebarOpen = ref(false)
const currentLocale = ref(getLocale())

// 拖拽调整大小相关
const isResizing = ref(false)
const sidebarWidth = ref(260) // 默认宽度
const mainContent = ref(null)

// Base62 编码函数
const toBase62 = (num) => {
  const chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
  if (num === 0) return '0'
  
  let result = ''
  while (num > 0) {
    result = chars[num % 62] + result
    num = Math.floor(num / 62)
  }
  return result
}

onMounted(async () => {
  // 从 localStorage 加载保存的宽度
  const savedWidth = localStorage.getItem('sidebarWidth')
  if (savedWidth) {
    sidebarWidth.value = parseInt(savedWidth)
    updateSidebarWidth()
  }

  try {
    const response = await axios.get('/api/auth/check')
    if (response.data.authenticated) {
      isAuthenticated.value = true
      userName.value = response.data.fullName || response.data.username
      userId.value = response.data.username || 'user_' + Math.random().toString(36).substr(2, 9)
      userBio.value = response.data.bio || 'Love learning, improve every day ✨'
      
      // 模拟一些统计数据（实际应该从后端获取）
      studyDays.value = Math.floor(Math.random() * 100) + 1
    } else {
      isAuthenticated.value = false
      userName.value = 'Guest User'
      userId.value = 'guest_' + toBase62(Date.now())
      userBio.value = ''
      studyDays.value = 0
    }
  } catch (error) {
    console.error('Failed to fetch user info:', error)
    isAuthenticated.value = false
    userName.value = 'Guest User'
    userId.value = 'guest_' + toBase62(Date.now())
    userBio.value = ''
    studyDays.value = 0
  }
  
  // 加载视频库数量
  loadVideoLibraryCount()
})

onBeforeUnmount(() => {
  // 清理事件监听器
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleResize)
  document.removeEventListener('touchend', stopResize)
})

const getAvatarText = () => {
  if (userName.value && userName.value.length > 0) {
    return userName.value.charAt(0).toUpperCase()
  }
  return '?'
}

const toggleMobileSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
  if (sidebarOpen.value) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
}

const closeMobileSidebar = () => {
  sidebarOpen.value = false
  document.body.style.overflow = ''
}

const handleLogin = () => {
  router.push('/login')
}

const handleLogout = async () => {
  if (confirm(t('auth.logoutConfirm'))) {
    try {
      await logout()
      router.push('/login')
    } catch (error) {
      console.error('Logout failed:', error)
      alert(t('auth.logoutFailed'))
    }
  }
}

const handleSettings = () => {
  alert(t('settings.developing'))
}

const changeLanguage = () => {
  setLocale(currentLocale.value)
}

// 拖拽调整大小功能
const startResize = (e) => {
  isResizing.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  
  // 添加事件监听器
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  document.addEventListener('touchmove', handleResize)
  document.addEventListener('touchend', stopResize)
  
  e.preventDefault()
}

const handleResize = (e) => {
  if (!isResizing.value) return
  
  // 获取鼠标/触摸位置
  const clientX = e.touches ? e.touches[0].clientX : e.clientX
  
  // 计算新宽度，限制在 200px 到 500px 之间
  const newWidth = Math.min(Math.max(clientX, 200), 500)
  sidebarWidth.value = newWidth
  
  updateSidebarWidth()
  
  e.preventDefault()
}

const stopResize = () => {
  if (!isResizing.value) return
  
  isResizing.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  
  // 移除事件监听器
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleResize)
  document.removeEventListener('touchend', stopResize)
  
  // 保存宽度到 localStorage
  localStorage.setItem('sidebarWidth', sidebarWidth.value.toString())
}

const updateSidebarWidth = () => {
  const sidebar = document.querySelector('.sidebar')
  if (sidebar) {
    sidebar.style.width = `${sidebarWidth.value}px`
  }
}

const loadVideoLibraryCount = async () => {
  try {
    const response = await axios.get('/api/youtube/videos', { params: { page: 0, size: 1 } })
    const data = response.data
    // 兼容分页格式 { totalElements } 或 直接数组
    completedVideos.value = typeof data.totalElements === 'number' ? data.totalElements : (Array.isArray(data) ? data.length : 0) || 0
  } catch (error) {
    console.error('Failed to load video library count:', error)
    completedVideos.value = 0
  }
}
</script>

<style scoped src="@/assets/styles/dashboard.css"></style>


