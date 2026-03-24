import api from '@/lib/axios'
import type { FollowUpResponse, MultiChannelFollowUpRequest } from '@/types/followup.types'

interface Page<T> {
  content: T[]
}

function base(orgId: string, invoiceId: string) {
  return `/api/v1/organizations/${orgId}/invoices/${invoiceId}/followups`
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
