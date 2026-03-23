import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import {
  listCustomers,
  createCustomer,
  updateCustomer,
  deleteCustomer,
  enableAutomation,
  disableAutomation,
} from '@/api/customer.api'
import type { CustomerRequest, ListCustomersParams } from '@/types/customer.types'

export function useClients(params?: ListCustomersParams) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['customers', orgId, params],
    queryFn:  () => listCustomers(orgId, params),
    enabled:  !!orgId,
  })
}

export function useAllClients() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['customers', orgId, 'all'],
    queryFn:  () => listCustomers(orgId, { size: 500 }),
    enabled:  !!orgId,
    staleTime: 60_000,
  })
}

function invalidateAll(qc: ReturnType<typeof useQueryClient>, orgId: string) {
  qc.invalidateQueries({ queryKey: ['customers', orgId] })
}

export function useCreateClient() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (body: CustomerRequest) => createCustomer(orgId, body),
    onSuccess:  () => invalidateAll(qc, orgId),
  })
}

export function useUpdateClient() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: Partial<CustomerRequest> }) =>
      updateCustomer(orgId, id, body),
    onSuccess: () => invalidateAll(qc, orgId),
  })
}

export function useDeleteClient() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => deleteCustomer(orgId, id),
    onSuccess:  () => invalidateAll(qc, orgId),
  })
}

export function useToggleAutomation() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      enabled ? enableAutomation(orgId, id) : disableAutomation(orgId, id),
    onSuccess: () => invalidateAll(qc, orgId),
  })
}
