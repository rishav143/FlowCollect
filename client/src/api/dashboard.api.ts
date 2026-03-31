import api from '@/lib/axios'

export interface NeedsAttentionItem {
  invoiceId:       string
  invoiceNumber:   string
  customerId:      string | null
  customerName:    string | null
  dueDate:         string | null   // ISO date string
  remainingAmount: number
  daysPastDue:     number
}

export interface DashboardStatsResponse {
  needsAttention: NeedsAttentionItem[]
}

export async function getDashboardStats(orgId: string): Promise<DashboardStatsResponse> {
  const { data } = await api.get<DashboardStatsResponse>(
    `/api/v1/organizations/${orgId}/dashboard/stats`,
  )
  return data
}
