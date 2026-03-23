import api from '@/lib/axios'
import type { CustomerResponse, CustomerRequest, ListCustomersParams } from '@/types/customer.types'

interface Page<T> {
  content:       T[]
  totalElements: number
  totalPages:    number
  number:        number
  size:          number
}

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}/customers`
}

export async function listCustomers(
  orgId: string,
  params?: ListCustomersParams,
): Promise<Page<CustomerResponse>> {
  const { data } = await api.get<Page<CustomerResponse>>(base(orgId), { params })
  return data
}

export async function getCustomer(orgId: string, id: string): Promise<CustomerResponse> {
  const { data } = await api.get<CustomerResponse>(`${base(orgId)}/${id}`)
  return data
}

export async function createCustomer(
  orgId: string,
  body: CustomerRequest,
): Promise<CustomerResponse> {
  const { data } = await api.post<CustomerResponse>(base(orgId), body)
  return data
}

export async function updateCustomer(
  orgId: string,
  id: string,
  body: Partial<CustomerRequest>,
): Promise<CustomerResponse> {
  const { data } = await api.patch<CustomerResponse>(`${base(orgId)}/${id}`, body)
  return data
}

export async function deleteCustomer(orgId: string, id: string): Promise<void> {
  await api.delete(`${base(orgId)}/${id}`)
}
