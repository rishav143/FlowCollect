import axios from 'axios'
import publicApi from '@/lib/publicAxios'
import type { AuthResponse } from '@/api/auth.api'

export interface InviteViewResponse {
  organizationName: string
  inviterName:      string
  email:            string
  role:             'ADMIN' | 'STAFF'
  expired:          boolean
}

export interface AcceptInviteRequest {
  name:     string
  password: string
}

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
  autoRecoveryEnabled:   boolean
}

function apiBase() {
  return import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
}

export async function getInviteView(token: string): Promise<InviteViewResponse> {
  const { data } = await publicApi.get<InviteViewResponse>(
    `/api/v1/public/invites/${token}`,
  )
  return data
}

/**
 * Accepts the invitation and returns a full AuthResponse (token + user + org)
 * so the caller can set the auth store and auto-login the new user.
 */
export async function acceptInvite(
  token: string,
  body: AcceptInviteRequest,
): Promise<AuthResponse> {
  const { data: lr } = await publicApi.post<BackendLoginResponse>(
    `/api/v1/public/invites/${token}/accept`,
    body,
  )

  const { data: org } = await axios.get<BackendOrgResponse>(
    `${apiBase()}/api/v1/organizations/${lr.organizationId}`,
    { headers: { Authorization: `Bearer ${lr.token}` } },
  )

  return {
    token: lr.token,
    user: { id: String(lr.id), name: lr.name, email: lr.email, role: lr.role },
    org: {
      id:                    String(org.id),
      name:                  org.name,
      currency:              org.currency,
      timezone:              org.timezone,
      paymentCollectionMode: org.paymentCollectionMode,
      autoRecoveryEnabled:   org.autoRecoveryEnabled ?? false,
    },
  }
}
