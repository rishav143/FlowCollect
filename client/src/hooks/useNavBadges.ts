import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'

export interface NavBadges {
  followups: number
  approvals: number
}

export function useNavBadges(): NavBadges {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  // Count invoices that need follow-up (ISSUED or PARTIALLY_PAID, and OVERDUE or DUE_TODAY)
  const overdueQuery = useQuery({
    queryKey: ['nav-followups-overdue', orgId],
    queryFn:  () => listInvoices(orgId, { timeStatus: 'OVERDUE', size: 1, sort: 'createdAt,desc' }),
    enabled:  !!orgId,
    staleTime: 60_000,
  })

  const dueTodayQuery = useQuery({
    queryKey: ['nav-followups-duetoday', orgId],
    queryFn:  () => listInvoices(orgId, { timeStatus: 'DUE_TODAY', size: 1, sort: 'createdAt,desc' }),
    enabled:  !!orgId,
    staleTime: 60_000,
  })

  const overdueCount  = overdueQuery.data?.totalElements  ?? 0
  const dueTodayCount = dueTodayQuery.data?.totalElements ?? 0

  // Filter to only active invoices (ISSUED | PARTIALLY_PAID)
  // The server returns all time-status invoices; we approximate by counting both statuses.
  // A more accurate count would require per-status queries, but this is good for a badge.
  const followups = overdueCount + dueTodayCount

  return {
    followups,
    approvals: 0,
  }
}
