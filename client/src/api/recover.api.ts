import api from '@/lib/axios'

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}`
}

// ---------------------------------------------------------------------------
// Stats
// ---------------------------------------------------------------------------

export interface RecoverStatsResponse {
  totalRecovered: number
  activeRules:    number
  sentToday:      number
  pendingToday:   number
  sentThisWeek:   number
}

export async function getRecoverStats(orgId: string): Promise<RecoverStatsResponse> {
  const { data } = await api.get<RecoverStatsResponse>(`${base(orgId)}/recover/stats`)
  return data
}

// ---------------------------------------------------------------------------
// Queue + Activity
// ---------------------------------------------------------------------------

export type FollowUpChannel = 'EMAIL' | 'SMS' | 'WHATSAPP'
export type FollowUpStatus  = 'SENT' | 'CANCELLED'

export interface UpcomingItem {
  invoiceId:     string
  invoiceNumber: string
  customerName:  string
  channel:       FollowUpChannel
  scheduledDate: string    // ISO local date
  ruleName:      string | null
  daysUntil:     number
}

export interface ActivityItem {
  followUpId:    string
  invoiceNumber: string
  customerName:  string
  channel:       FollowUpChannel
  status:        FollowUpStatus
  ruleName:      string | null
  eventAt:       string    // ISO instant
}

export interface QueueActivityResponse {
  upcoming:      UpcomingItem[]
  totalUpcoming: number
  activity:      ActivityItem[]
}

export async function getQueueActivity(orgId: string): Promise<QueueActivityResponse> {
  const { data } = await api.get<QueueActivityResponse>(`${base(orgId)}/recover/queue-activity`)
  return data
}

// ---------------------------------------------------------------------------
// Skip (cancel) a queued follow-up
// ---------------------------------------------------------------------------

export async function skipFollowUp(orgId: string, invoiceId: string, followUpId: string): Promise<void> {
  await api.patch(`${base(orgId)}/invoices/${invoiceId}/followups/${followUpId}/cancel`)
}

// ---------------------------------------------------------------------------
// Toggle auto-recovery on/off for the org
// ---------------------------------------------------------------------------

export async function setAutoRecovery(orgId: string, enabled: boolean): Promise<void> {
  await api.patch(`${base(orgId)}`, { autoRecoveryEnabled: enabled })
}
