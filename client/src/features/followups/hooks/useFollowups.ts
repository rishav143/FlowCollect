import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'
import { listFollowups, dispatchFollowup } from '@/api/followup.api'
import type { MultiChannelFollowUpRequest } from '@/types/followup.types'
import type { TimeStatus } from '@/types/invoice.types'

export type FollowupFilter = 'ALL' | 'OVERDUE' | 'DUE_TODAY' | 'UPCOMING'

// Returns active invoices (ISSUED | PARTIALLY_PAID) filtered by time status
export function useFollowupInvoices(filter: FollowupFilter) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['followup-invoices', orgId, filter],
    queryFn: () => {
      const timeStatus: TimeStatus | undefined =
        filter === 'OVERDUE'   ? 'OVERDUE'   :
        filter === 'DUE_TODAY' ? 'DUE_TODAY' :
        filter === 'UPCOMING'  ? 'NOT_DUE'   :
        undefined

      return listInvoices(orgId, {
        lifeCycleStatus: undefined,
        timeStatus,
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

export function useInvoiceFollowups(invoiceId: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['followups', orgId, invoiceId],
    queryFn:  () => listFollowups(orgId, invoiceId),
    enabled:  !!orgId && !!invoiceId,
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
    },
  })
}
