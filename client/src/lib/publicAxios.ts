import axios from 'axios'

/**
 * Unauthenticated axios instance for public endpoints (no JWT, no auth redirect).
 * Used by customer-facing pages like the payment confirmation page.
 */
const publicApi = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
})

export default publicApi
