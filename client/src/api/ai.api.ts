import api from '@/lib/axios'

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}/ai`
}

export async function getAiOverviewInsights(orgId: string): Promise<string> {
  const { data } = await api.get<{ insights: string }>(`${base(orgId)}/insights/overview`)
  return data.insights
}
