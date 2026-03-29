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

// On 401/403 — token expired or invalid → clear auth and redirect to login
// On 404 for org-scoped routes — token points to a different DB/org → force re-login
// Skip redirect for auth endpoints (login/register) so their own error handlers run
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url    = (err.config?.url ?? '') as string
    const status = err.response?.status as number | undefined
    const isAuth = url.includes('/auth/')

    const forceLogout =
      !isAuth &&
      (status === 401 ||
        status === 403 ||
        // Org root not found in this DB — stale token from another environment.
        // Only match the org root itself (e.g. GET /organizations/{id}), NOT
        // sub-resources like templates or invoices — those have their own 404 handling.
        (status === 404 && /\/organizations\/[^/]+$/.test(url)))

    if (forceLogout) {
      useAuthStore.getState().clearAuth()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export default api
