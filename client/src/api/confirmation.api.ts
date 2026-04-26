import api from '@/lib/axios'
import type { PaymentConfirmationResponse, ConfirmationStatus } from '@/types/confirmation.types'

interface Page<T> {
  content:       T[]
  totalElements: number
  totalPages:    number
  number:        number
  size:          number
}

interface ReviewRequest {
  note?:       string   // matches backend ReviewConfirmationRequest.note
  newDueDate?: string   // ISO date YYYY-MM-DD — only used for request-remaining
}

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}/payment-confirmations`
}

export async function listConfirmations(
  orgId: string,
  params?: { status?: ConfirmationStatus; page?: number; size?: number; sort?: string },
): Promise<Page<PaymentConfirmationResponse>> {
  const { data } = await api.get<Page<PaymentConfirmationResponse>>(base(orgId), { params })
  return data
}

export async function approveConfirmation(
  orgId: string,
  confirmationId: string,
  body?: ReviewRequest,
): Promise<PaymentConfirmationResponse> {
  const { data } = await api.post<PaymentConfirmationResponse>(
    `${base(orgId)}/${confirmationId}/approve`,
    body,
  )
  return data
}

export async function requestRemainingConfirmation(
  orgId: string,
  confirmationId: string,
  body?: ReviewRequest,
): Promise<PaymentConfirmationResponse> {
  const { data } = await api.post<PaymentConfirmationResponse>(
    `${base(orgId)}/${confirmationId}/request-remaining`,
    body,
  )
  return data
}

export async function rejectConfirmation(
  orgId: string,
  confirmationId: string,
  body?: ReviewRequest,
): Promise<PaymentConfirmationResponse> {
  const { data } = await api.post<PaymentConfirmationResponse>(
    `${base(orgId)}/${confirmationId}/reject`,
    body,
  )
  return data
}
