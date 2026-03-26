import axios from 'axios'
import { useAuthStore } from '@/store/auth.store'
import { registerDevMock } from './devMock'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
})

// Register dev mock before auth interceptor so mocked requests never hit the network
registerDevMock(api)

// Attach Bearer token to every request
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// On 401 — token expired or invalid → clear auth and redirect to login.
// Only triggers when the server actually responds (not on network errors /
// cold-start timeouts). Auth endpoints handle their own 401 errors.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url         = (err.config?.url ?? '') as string
    const status      = err.response?.status
    const hasResponse = !!err.response   // false on network error / no connection

    if (status === 401 && hasResponse && !url.includes('/auth/')) {
      useAuthStore.getState().clearAuth()
      window.location.href = '/login'
    }

    return Promise.reject(err)
  },
)

export default api
