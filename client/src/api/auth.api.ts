import api from '@/lib/axios'
import type { User, Org } from '@/types/auth.types'

export interface AuthResponse {
  token: string
  user:  User
  org:   Org
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/v1/auth/login', { email, password })
  return data
}

export async function register(params: {
  name:     string
  email:    string
  password: string
  orgName:  string
  currency: string
}): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/v1/auth/register', params)
  return data
}
