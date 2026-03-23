import axios from 'axios'
import { useAuthStore } from '@/store/auth.store'
import { registerDevMock } from './devMock'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
})

// Register dev mock before auth interceptor so mocked requests never hit the network
registerDevMock(api)

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default api
