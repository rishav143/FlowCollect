import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'
import type { ListInvoicesParams } from '@/types/invoice.types'

export function useInvoices(params?: ListInvoicesParams) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['invoices', orgId, params],
    queryFn:  () => listInvoices(orgId, params),
    enabled:  !!orgId,
  })
}
