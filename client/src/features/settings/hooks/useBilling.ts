import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { getBilling, purchaseCredits } from '@/api/organization.api'
import type { PurchaseRequest } from '@/api/organization.api'

const qk = (orgId: string) => ['billing', orgId]

export function useBilling() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: qk(orgId),
    queryFn:  () => getBilling(orgId),
    enabled:  !!orgId,
  })
}

export function usePurchaseCredits() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()
  return useMutation({
    mutationFn: (body: PurchaseRequest) => purchaseCredits(orgId, body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: qk(orgId) }),
  })
}
