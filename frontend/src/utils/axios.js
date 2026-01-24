import axios from 'axios'

// 根据环境使用不同的 baseURL
// 生产环境使用相对路径（前后端同域），开发环境使用 localhost
const instance = axios.create({
  baseURL: import.meta.env.PROD ? '' : 'http://localhost:8081',
  timeout: 10000,
  withCredentials: true
})

// 请求拦截器
instance.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      // 未授权，跳转到登录页
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default instance


