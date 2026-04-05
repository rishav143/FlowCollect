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

export interface AiTemplateResult {
  subject: string | null
  body:    string
}

export async function generateTemplate(
  orgId:   string,
  channel: string,
  tone:    string,
): Promise<AiTemplateResult> {
  const { data } = await api.post<AiTemplateResult>(
    `${base(orgId)}/templates/generate`,
    { channel, tone },
  )
  return data
}

export async function enhanceTemplate(
  orgId:    string,
  channel:  string,
  tone:     string,
  body:     string,
  subject?: string,
): Promise<AiTemplateResult> {
  const { data } = await api.post<AiTemplateResult>(
    `${base(orgId)}/templates/enhance`,
    { channel, tone, body, subject: subject ?? null },
  )
  return data
}
