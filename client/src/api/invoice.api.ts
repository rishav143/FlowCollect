import api from '@/lib/axios'
import type { InvoiceResponse, ListInvoicesParams } from '@/types/invoice.types'

interface Page<T> {
  content:       T[]
  totalElements: number
  totalPages:    number
  number:        number
  size:          number
}

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}/invoices`
}

export async function listInvoices(
  orgId: string,
  params?: ListInvoicesParams,
): Promise<Page<InvoiceResponse>> {
  const { data } = await api.get<Page<InvoiceResponse>>(base(orgId), { params })
  return data
}

export async function getInvoice(orgId: string, id: string): Promise<InvoiceResponse> {
  const { data } = await api.get<InvoiceResponse>(`${base(orgId)}/${id}`)
  return data
}

export interface CreateInvoiceBody {
  invoiceNumber:  string
  customerId?:    string
  dueDate?:       string
  taxPercentage?: number
  items:          { description: string; quantity: number; unitPrice: number }[]
}

export interface UpdateInvoiceBody {
  invoiceNumber?:  string
  customerId?:     string
  dueDate?:        string
  taxPercentage?:  number
  items?:          { description: string; quantity: number; unitPrice: number }[]
}

export async function createInvoice(
  orgId: string,
  body: CreateInvoiceBody,
): Promise<InvoiceResponse> {
  const { data } = await api.post<InvoiceResponse>(base(orgId), body)
  return data
}

export async function updateInvoice(
  orgId: string,
  id: string,
  body: UpdateInvoiceBody,
): Promise<InvoiceResponse> {
  const { data } = await api.patch<InvoiceResponse>(`${base(orgId)}/${id}`, body)
  return data
}

export async function deleteInvoice(orgId: string, id: string): Promise<void> {
  await api.delete(`${base(orgId)}/${id}`)
}

export async function issueInvoice(
  orgId: string,
  id: string,
  issueDate?: string,
): Promise<InvoiceResponse> {
  const { data } = await api.post<InvoiceResponse>(
    `${base(orgId)}/${id}/issue`,
    issueDate ? { issueDate } : undefined,
  )
  return data
}

export async function cancelInvoice(orgId: string, id: string): Promise<InvoiceResponse> {
  const { data } = await api.post<InvoiceResponse>(`${base(orgId)}/${id}/cancel`)
  return data
}

export async function downloadInvoicePdf(orgId: string, id: string): Promise<Blob> {
  const { data } = await api.get<Blob>(`${base(orgId)}/${id}/pdf`, {
    responseType: 'blob',
  })
  return data
}
