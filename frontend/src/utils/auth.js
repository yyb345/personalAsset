import axios from './axios'

export async function checkAuth() {
  try {
    const response = await axios.get('/api/auth/check')
    return response.data.authenticated
  } catch (error) {
    return false
  }
}

export async function login(username, password) {
  const response = await axios.post('/api/auth/login', { username, password })
  return response.data
}

export async function register(userData) {
  const response = await axios.post('/api/auth/register', userData)
  return response.data
}

export async function logout() {
  await axios.post('/api/auth/logout')
}



