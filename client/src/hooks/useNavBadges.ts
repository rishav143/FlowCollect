import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'
import { listConfirmations } from '@/api/confirmation.api'

export interface NavBadges {
  followups: number
  approvals: number
}

export function useNavBadges(): NavBadges {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  const overdueQuery = useQuery({
    queryKey: ['nav-followups-overdue', orgId],
    queryFn:  () => listInvoices(orgId, { timeStatus: 'OVERDUE',   size: 1 }),
    enabled:  !!orgId,
    staleTime: 60_000,
  })

  const dueTodayQuery = useQuery({
    queryKey: ['nav-followups-duetoday', orgId],
    queryFn:  () => listInvoices(orgId, { timeStatus: 'DUE_TODAY', size: 1 }),
    enabled:  !!orgId,
    staleTime: 60_000,
  })

  const approvalsQuery = useQuery({
    queryKey: ['nav-approvals', orgId],
    queryFn:  () => listConfirmations(orgId, { status: 'PENDING_APPROVAL', size: 1 }),
    enabled:  !!orgId,
    staleTime: 60_000,
  })

  return {
    followups: (overdueQuery.data?.totalElements ?? 0) + (dueTodayQuery.data?.totalElements ?? 0),
    approvals: approvalsQuery.data?.totalElements ?? 0,
  }
}
