import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { getBilling } from '@/api/organization.api'

const qk = (orgId: string) => ['billing', orgId]

export function useBilling() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: qk(orgId),
    queryFn:  () => getBilling(orgId),
    enabled:  !!orgId,
  })
}
