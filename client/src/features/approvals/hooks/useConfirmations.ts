import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import {
  listConfirmations,
  approveConfirmation,
  rejectConfirmation,
  requestRemainingConfirmation,
} from '@/api/confirmation.api'
import type { ConfirmationStatus } from '@/types/confirmation.types'

export type ApprovalFilter = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'REMAINING_REQUESTED' | 'ALL'

export function useConfirmations(filter: ApprovalFilter) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey:       ['confirmations', orgId, filter],
    queryFn:        () =>
      listConfirmations(orgId, {
        status: filter === 'ALL' ? undefined : (filter as ConfirmationStatus),
        size: 100,
        sort: 'createdAt,desc',
      }),
    enabled:        !!orgId,
    refetchInterval: 30_000,   // poll every 30s so new claims appear without a reload
  })
}

export function usePendingConfirmationCount() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey:        ['confirmations-count', orgId],
    queryFn:         () => listConfirmations(orgId, { status: 'PENDING_APPROVAL', size: 1 }),
    enabled:         !!orgId,
    select:          (d) => d.totalElements,
    refetchInterval: 30_000,   // keep nav badge in sync
  })
}

function useReviewMutation(
  action: (orgId: string, id: string, body?: { businessNote?: string; newDueDate?: string }) => Promise<unknown>,
) {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const queryClient = useQueryClient()

  return useMutation({
    // ApprovalsPage and ActionTable pass onError at the call site and show
    // inline feedback — mark silent so the global MutationCache handler doesn't
    // also fire a toast on top of that.
    meta: { silent: true },
    mutationFn: ({ id, businessNote, newDueDate }: { id: string; businessNote?: string; newDueDate?: string }) => {
      const body = (businessNote || newDueDate)
        ? { ...(businessNote ? { businessNote } : {}), ...(newDueDate ? { newDueDate } : {}) }
        : undefined
      return action(orgId, id, body)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['confirmations', orgId] })
      queryClient.invalidateQueries({ queryKey: ['confirmations-count', orgId] })
      queryClient.invalidateQueries({ queryKey: ['nav-approvals', orgId] })
    },
  })
}

export function useApproveConfirmation()          { return useReviewMutation(approveConfirmation) }
export function useRejectConfirmation()           { return useReviewMutation(rejectConfirmation) }
export function useRequestRemainingConfirmation() { return useReviewMutation(requestRemainingConfirmation) }
