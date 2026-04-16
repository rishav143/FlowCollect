import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'
import { listFollowups, dispatchFollowup } from '@/api/followup.api'
import type { MultiChannelFollowUpRequest, FollowUpResponse } from '@/types/followup.types'
import type { TimeStatus } from '@/types/invoice.types'

export type FollowupFilter = 'ALL' | 'OVERDUE' | 'DUE_TODAY' | 'UPCOMING' | 'LEAST_CONTACTED'

// Returns active invoices (ISSUED | PARTIALLY_PAID) filtered by time status
export function useFollowupInvoices(filter: FollowupFilter) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['followup-invoices', orgId, filter],
    queryFn: () => {
      const timeStatus: TimeStatus | undefined =
        filter === 'OVERDUE'          ? 'OVERDUE'   :
        filter === 'DUE_TODAY'        ? 'DUE_TODAY' :
        filter === 'UPCOMING'         ? 'NOT_DUE'   :
        undefined // ALL and LEAST_CONTACTED fetch everything

      return listInvoices(orgId, {
        lifeCycleStatus: undefined,
        timeStatus,
        customerActive: true,
        size: 200,
        sort: 'dueDate,asc',
      }).then((page) =>
        page.content.filter(
          (inv) =>
            inv.lifeCycleStatus === 'ISSUED' ||
            inv.lifeCycleStatus === 'PARTIALLY_PAID',
        ),
      )
    },
    enabled: !!orgId,
  })
}

// Returns a map of invoiceId → last FollowUpResponse (sorted by sentAt desc)
export function useFollowupsByInvoices(invoiceIds: string[]): Record<string, FollowUpResponse> {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  const results = useQueries({
    queries: invoiceIds.map((id) => ({
      queryKey: ['followups', orgId, id],
      queryFn:  () => listFollowups(orgId, id),
      enabled:  !!orgId,
    })),
  })

  const map: Record<string, FollowUpResponse> = {}
  invoiceIds.forEach((id, i) => {
    const list = results[i]?.data
    if (list && list.length > 0) {
      // Only count successfully SENT follow-ups as "last contacted"
      const sent = list.filter((f) => f.status === 'SENT')
      if (sent.length === 0) return
      const sorted = [...sent].sort(
        (a, b) => new Date(b.sentAt ?? b.createdAt).getTime() - new Date(a.sentAt ?? a.createdAt).getTime(),
      )
      map[id] = sorted[0]
    }
  })
  return map
}

// Returns a map of invoiceId → all FollowUpResponse[] (shared query keys with useFollowupsByInvoices — no extra requests)
export function useAllFollowupsByInvoices(invoiceIds: string[]): Record<string, FollowUpResponse[]> {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  const results = useQueries({
    queries: invoiceIds.map((id) => ({
      queryKey: ['followups', orgId, id],
      queryFn:  () => listFollowups(orgId, id),
      enabled:  !!orgId,
    })),
  })

  const map: Record<string, FollowUpResponse[]> = {}
  invoiceIds.forEach((id, i) => {
    const list = results[i]?.data
    if (list) map[id] = list
  })
  return map
}

export function useInvoiceFollowups(invoiceId: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['followups', orgId, invoiceId],
    queryFn:  () => listFollowups(orgId, invoiceId),
    enabled:  !!orgId && !!invoiceId,
    staleTime: 0,
  })
}

export function useDispatchFollowup(invoiceId: string) {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (body: MultiChannelFollowUpRequest) =>
      dispatchFollowup(orgId, invoiceId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['followups', orgId, invoiceId] })
      queryClient.invalidateQueries({ queryKey: ['followup-invoices', orgId] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })
    },
  })
}
