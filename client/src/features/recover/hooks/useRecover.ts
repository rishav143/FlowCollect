import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { getRecoverStats, getQueueActivity, skipFollowUp, setAutoRecovery } from '@/api/recover.api'
import { listReminderRules } from '@/api/reminder.api'

// ---------------------------------------------------------------------------
// Query key factories — isolated from other features
// ---------------------------------------------------------------------------

const statsQk    = (orgId: string) => ['recover-stats', orgId]
const rulesQk    = (orgId: string) => ['reminder-rules', orgId]  // shared with Reminders page
const queueQk    = (orgId: string) => ['recover-queue', orgId]

// ---------------------------------------------------------------------------
// Stats
// ---------------------------------------------------------------------------

export function useRecoverStats() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: statsQk(orgId),
    queryFn:  () => getRecoverStats(orgId),
    enabled:  !!orgId,
    // Refresh every 30s — sent/pending counts change as scheduler runs
    refetchInterval: 30_000,
    retry: false,  // silent failure — don't crash the page
  })
}

// ---------------------------------------------------------------------------
// AUTO rules — filtered from the shared rules list
// ---------------------------------------------------------------------------

export function useAutoRules() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: rulesQk(orgId),
    queryFn:  () => listReminderRules(orgId),
    enabled:  !!orgId,
    select:   (data) => data.content.filter((r) => r.mode === 'AUTO'),
  })
}

// ---------------------------------------------------------------------------
// Queue + Activity
// ---------------------------------------------------------------------------

export function useQueueActivity() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: queueQk(orgId),
    queryFn:  () => getQueueActivity(orgId),
    enabled:  !!orgId,
    refetchInterval: 30_000,
    retry: false,
  })
}

export function useSkipFollowUp() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: ({ invoiceId, followUpId }: { invoiceId: string; followUpId: string }) =>
      skipFollowUp(orgId, invoiceId, followUpId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queueQk(orgId) })
      qc.invalidateQueries({ queryKey: statsQk(orgId) })
    },
  })
}

// ---------------------------------------------------------------------------
// Toggle auto-recovery for the org
// ---------------------------------------------------------------------------

export function useToggleAutoRecovery() {
  const orgId  = useAuthStore((s) => s.org?.id ?? '')
  const setAuth = useAuthStore((s) => s.setAuth)
  const token   = useAuthStore((s) => s.token)
  const user    = useAuthStore((s) => s.user)
  const org     = useAuthStore((s) => s.org)
  const qc      = useQueryClient()

  return useMutation({
    mutationFn: (enabled: boolean) => setAutoRecovery(orgId, enabled),
    onSuccess: (_data, enabled) => {
      // Optimistically update the org in the auth store so the toggle
      // reflects immediately without a full profile re-fetch
      if (token && user && org) {
        setAuth(token, user, { ...org, autoRecoveryEnabled: enabled })
      }
      // Invalidate stats so KPI cards refresh
      qc.invalidateQueries({ queryKey: statsQk(orgId) })
    },
  })
}
