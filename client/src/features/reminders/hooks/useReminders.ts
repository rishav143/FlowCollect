import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import {
  listReminderRules,
  createReminderRule,
  updateReminderRule,
  deleteReminderRule,
  toggleReminderRule,
} from '@/api/reminder.api'
import type { ReminderRuleRequest } from '@/types/reminder.types'

const qk = (orgId: string) => ['reminder-rules', orgId]

export function useReminderRules() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: qk(orgId),
    queryFn:  () => listReminderRules(orgId),
    enabled:  !!orgId,
  })
}

function useInvalidate() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()
  return () => qc.invalidateQueries({ queryKey: qk(orgId) })
}

export function useCreateReminderRule() {
  const orgId      = useAuthStore((s) => s.org?.id ?? '')
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (body: ReminderRuleRequest) => createReminderRule(orgId, body),
    onSuccess:  invalidate,
  })
}

export function useUpdateReminderRule() {
  const orgId      = useAuthStore((s) => s.org?.id ?? '')
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: Partial<ReminderRuleRequest> }) =>
      updateReminderRule(orgId, id, body),
    onSuccess: invalidate,
  })
}

export function useDeleteReminderRule() {
  const orgId      = useAuthStore((s) => s.org?.id ?? '')
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (id: string) => deleteReminderRule(orgId, id),
    onSuccess:  invalidate,
  })
}

export function useToggleReminderRule() {
  const orgId      = useAuthStore((s) => s.org?.id ?? '')
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      toggleReminderRule(orgId, id, active),
    onSuccess: invalidate,
  })
}
