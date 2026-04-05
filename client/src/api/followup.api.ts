import api from '@/lib/axios'
import type { FollowUpResponse, MultiChannelFollowUpRequest } from '@/types/followup.types'
import type { FollowUpChannel, FollowUpStatus } from '@/types/followup.types'

interface Page<T> {
  content: T[]
}

function base(orgId: string, invoiceId: string) {
  return `/api/v1/organizations/${orgId}/invoices/${invoiceId}/followups`
}

// ---------------------------------------------------------------------------
// Consolidated dispatch
// ---------------------------------------------------------------------------

export interface ConsolidatedFollowUpRequest {
  invoiceIds:  string[]
  channels:    FollowUpChannel[]
  templateId?: string
}

export interface ConsolidatedFollowUpResponse {
  channel:           FollowUpChannel
  status:            FollowUpStatus
  externalMessageId: string | null
  sentAt:            string | null
  followUpIds:       string[]
  invoiceIds:        string[]
  errorMessage:      string | null
}

export async function consolidatedDispatch(
  orgId:   string,
  request: ConsolidatedFollowUpRequest,
): Promise<ConsolidatedFollowUpResponse[]> {
  const { data } = await api.post<ConsolidatedFollowUpResponse[]>(
    `/api/v1/organizations/${orgId}/followups/consolidated`,
    request,
  )
  return data
}

export async function listFollowups(
  orgId: string,
  invoiceId: string,
): Promise<FollowUpResponse[]> {
  const { data } = await api.get<Page<FollowUpResponse>>(base(orgId, invoiceId))
  return data.content
}

export async function dispatchFollowup(
  orgId: string,
  invoiceId: string,
  body: MultiChannelFollowUpRequest,
): Promise<FollowUpResponse[]> {
  const { data } = await api.post<FollowUpResponse[]>(
    `${base(orgId, invoiceId)}/dispatch`,
    body,
  )
  return data
}
