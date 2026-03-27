import api from '@/lib/axios'
import type { ReminderRuleResponse, ReminderRuleRequest } from '@/types/reminder.types'

interface Page<T> {
  content:       T[]
  totalElements: number
}

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}/reminder-rules`
}

export async function listReminderRules(orgId: string): Promise<Page<ReminderRuleResponse>> {
  const { data } = await api.get<Page<ReminderRuleResponse>>(base(orgId))
  return data
}

export async function createReminderRule(orgId: string, body: ReminderRuleRequest): Promise<ReminderRuleResponse> {
  const { data } = await api.post<ReminderRuleResponse>(base(orgId), body)
  return data
}

export async function updateReminderRule(orgId: string, id: string, body: Partial<ReminderRuleRequest>): Promise<ReminderRuleResponse> {
  const { data } = await api.patch<ReminderRuleResponse>(`${base(orgId)}/${id}`, body)
  return data
}

export async function deleteReminderRule(orgId: string, id: string): Promise<void> {
  await api.delete(`${base(orgId)}/${id}`)
}

export async function toggleReminderRule(orgId: string, id: string, active: boolean): Promise<ReminderRuleResponse> {
  const { data } = await api.patch<ReminderRuleResponse>(`${base(orgId)}/${id}/${active ? 'activate' : 'deactivate'}`)
  return data
}
