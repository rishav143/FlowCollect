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
// Toggle auto-recovery on/off for the org
// ---------------------------------------------------------------------------

export async function setAutoRecovery(orgId: string, enabled: boolean): Promise<void> {
  await api.patch(`${base(orgId)}`, { autoRecoveryEnabled: enabled })
}
