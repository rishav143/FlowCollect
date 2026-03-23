import api from '@/lib/axios'
import type { PaymentResponse, PaymentRequest } from '@/types/payment.types'

interface Page<T> {
  content:       T[]
  totalElements: number
  totalPages:    number
  number:        number
  size:          number
}

function base(orgId: string, invoiceId: string) {
  return `/api/v1/organizations/${orgId}/invoices/${invoiceId}/payments`
}

export async function listPayments(
  orgId: string,
  invoiceId: string,
): Promise<Page<PaymentResponse>> {
  const { data } = await api.get<Page<PaymentResponse>>(base(orgId, invoiceId))
  return data
}

export async function recordPayment(
  orgId: string,
  invoiceId: string,
  body: PaymentRequest,
): Promise<PaymentResponse> {
  const { data } = await api.post<PaymentResponse>(base(orgId, invoiceId), body)
  return data
}

export async function deletePayment(
  orgId: string,
  invoiceId: string,
  paymentId: string,
): Promise<void> {
  await api.delete(`${base(orgId, invoiceId)}/${paymentId}`)
}
