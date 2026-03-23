import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { getCustomer } from '@/api/customer.api'
import { listInvoices } from '@/api/invoice.api'

export function useClientDetail(id: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['customer', orgId, id],
    queryFn:  () => getCustomer(orgId, id),
    enabled:  !!orgId && !!id,
  })
}

export function useClientInvoices(customerId: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  // Fetch all invoices for this org, then filter by customerId client-side.
  // The server's invoice list doesn't expose a customerId filter in v1.
  return useQuery({
    queryKey: ['client-invoices', orgId, customerId],
    queryFn:  async () => {
      const page = await listInvoices(orgId, { size: 200, sort: 'createdAt,desc' })
      return page.content.filter((inv) => inv.customerId === customerId)
    },
    enabled: !!orgId && !!customerId,
  })
}
