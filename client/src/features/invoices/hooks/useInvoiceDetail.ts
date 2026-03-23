import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { getInvoice } from '@/api/invoice.api'

export function useInvoiceDetail(id: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['invoices', orgId, id],
    queryFn:  () => getInvoice(orgId, id),
    enabled:  !!orgId && !!id,
  })
}
