import api from '@/lib/axios'

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}/ai`
}

export interface AiOverviewInsight {
  insights:    string
  cached:      boolean
  generatedAt: string  // ISO Instant
}

export async function getAiOverviewInsights(
  orgId: string,
  forceRefresh = false,
): Promise<AiOverviewInsight> {
  const { data } = await api.get<AiOverviewInsight>(
    `${base(orgId)}/insights/overview`,
    forceRefresh ? { params: { forceRefresh: true } } : undefined,
  )
  return data
}
