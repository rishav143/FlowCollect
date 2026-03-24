import axios from 'axios'
import api from '@/lib/axios'
import type { User, Org } from '@/types/auth.types'

// ---------------------------------------------------------------------------
// Shared response type used by auth store
// ---------------------------------------------------------------------------

export interface AuthResponse {
  token: string
  user:  User
  org:   Org
}

// ---------------------------------------------------------------------------
// Backend response shapes (match Java DTOs exactly)
// ---------------------------------------------------------------------------

interface BackendLoginResponse {
  token:          string
  id:             string
  organizationId: string
  name:           string
  email:          string
  role:           'ADMIN' | 'STAFF'
}

interface BackendOrgResponse {
  id:                    string
  name:                  string
  currency:              string
  timezone:              string
  paymentCollectionMode: 'PAYMENT_LINK' | 'CONFIRMATION_FLOW'
}

export interface RegisterResult {
  organizationId:             string
  userId:                     string
  emailVerificationRequired:  boolean
  phoneVerificationRequired:  boolean
  message:                    string
}

// ---------------------------------------------------------------------------
// Auth API
// ---------------------------------------------------------------------------

function apiBase() {
  return import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
}

/**
 * Login — fetches org profile with the received token so we can
 * populate the auth store with the full Org shape in one step.
 */
export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data: lr } = await api.post<BackendLoginResponse>('/api/v1/auth/login', { email, password })

  // Fetch org using raw axios with the new token — bypasses store interceptor
  // since the token isn't in the store yet at this point.
  const { data: org } = await axios.get<BackendOrgResponse>(
    `${apiBase()}/api/v1/organizations/${lr.organizationId}`,
    { headers: { Authorization: `Bearer ${lr.token}` } },
  )

  return {
    token: lr.token,
    user: { id: String(lr.id), name: lr.name, email: lr.email, role: lr.role },
    org:  {
      id:                    String(org.id),
      name:                  org.name,
      currency:              org.currency,
      timezone:              org.timezone,
      paymentCollectionMode: org.paymentCollectionMode,
    },
  }
}

/**
 * Register — backend returns a verification-required response, NOT a token.
 * User must verify their email before they can log in.
 */
export async function register(params: {
  ownerName:        string
  organizationName: string
  email:            string
  password:         string
  currency:         string
  timezone:         string
}): Promise<RegisterResult> {
  const { data } = await api.post<RegisterResult>('/api/v1/auth/register', params)
  return data
}
