import api from '@/lib/axios'

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}`
}

// ---------------------------------------------------------------------------
// Org profile
// ---------------------------------------------------------------------------

export interface OrgProfileResponse {
  id:                    string
  name:                  string
  email:                 string
  currency:              string
  timezone:              string
  paymentCollectionMode: 'PAYMENT_LINK' | 'CONFIRMATION_FLOW'
}

export interface OrgProfileRequest {
  name?:                  string
  email?:                 string
  currency?:              string
  timezone?:              string
  paymentCollectionMode?: 'PAYMENT_LINK' | 'CONFIRMATION_FLOW' | 'SIMPLE'
}

export async function getOrgProfile(orgId: string): Promise<OrgProfileResponse> {
  const { data } = await api.get<OrgProfileResponse>(base(orgId))
  return data
}

export async function updateOrgProfile(orgId: string, body: OrgProfileRequest): Promise<OrgProfileResponse> {
  const { data } = await api.patch<OrgProfileResponse>(base(orgId), body)
  return data
}

// ---------------------------------------------------------------------------
// Team members
// ---------------------------------------------------------------------------

export type MemberType = 'USER' | 'INVITE'

export interface MemberResponse {
  id:         string
  name:       string
  email:      string
  role:       'ADMIN' | 'STAFF'
  status:     'ACTIVE' | 'INACTIVE' | 'PENDING'
  memberType: MemberType
  joinedAt:   string | null
  invitedAt:  string | null
}

export async function listMembers(orgId: string): Promise<MemberResponse[]> {
  const { data } = await api.get<MemberResponse[]>(`${base(orgId)}/members`)
  return data
}

export async function inviteMember(
  orgId: string,
  body: { email: string; role: 'ADMIN' | 'STAFF' },
): Promise<MemberResponse> {
  const { data } = await api.post<MemberResponse>(`${base(orgId)}/members/invite`, body)
  return data
}

export async function revokeInvite(orgId: string, inviteId: string): Promise<void> {
  await api.delete(`${base(orgId)}/members/invites/${inviteId}`)
}

export async function removeMember(orgId: string, userId: string): Promise<void> {
  await api.delete(`${base(orgId)}/members/${userId}`)
}

// ---------------------------------------------------------------------------
// Billing
// ---------------------------------------------------------------------------

export interface BillingResponse {
  plan:       'STARTER' | 'PRO' | 'BUSINESS'
  smsCredits: number
  waCredits:  number
}

export interface PurchaseRequest {
  channel: 'SMS' | 'WHATSAPP'
  pack:    number   // number of credits to add
}

export async function getBilling(orgId: string): Promise<BillingResponse> {
  const { data } = await api.get<BillingResponse>(`${base(orgId)}/billing`)
  return data
}

export async function purchaseCredits(orgId: string, body: PurchaseRequest): Promise<BillingResponse> {
  const { data } = await api.post<BillingResponse>(`${base(orgId)}/billing/credits/purchase`, body)
  return data
}
