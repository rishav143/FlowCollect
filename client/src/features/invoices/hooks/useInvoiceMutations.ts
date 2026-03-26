import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import {
  createInvoice,
  updateInvoice,
  deleteInvoice,
  issueInvoice,
  downloadInvoicePdf,
  archiveInvoice,
  unarchiveInvoice,
  archivePaidInvoices,
  bulkArchiveInvoices,
  type CreateInvoiceBody,
  type UpdateInvoiceBody,
} from '@/api/invoice.api'

export function useCreateInvoice() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (body: CreateInvoiceBody) => createInvoice(orgId, body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['invoices', orgId] }),
  })
}

export function useUpdateInvoice(invoiceId: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (body: UpdateInvoiceBody) => updateInvoice(orgId, invoiceId, body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['invoices', orgId] }),
  })
}

export function useDeleteInvoice() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => deleteInvoice(orgId, id),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['invoices', orgId] }),
  })
}

export function useIssueInvoice(invoiceId: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (issueDate?: string) => issueInvoice(orgId, invoiceId, issueDate),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['invoices', orgId] }),
  })
}

export function useArchiveInvoice() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => archiveInvoice(orgId, id),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['invoices', orgId] }),
  })
}

export function useUnarchiveInvoice() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => unarchiveInvoice(orgId, id),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['invoices', orgId] }),
  })
}

export function useArchivePaidInvoices() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: () => archivePaidInvoices(orgId),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['invoices', orgId] }),
  })
}

export function useBulkArchiveInvoices() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (ids: string[]) => bulkArchiveInvoices(orgId, ids),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['invoices', orgId] }),
  })
}

export function useDownloadPdf() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useMutation({
    mutationFn: async ({ id, invoiceNumber }: { id: string; invoiceNumber: string }) => {
      const blob = await downloadInvoicePdf(orgId, id)
      const url  = URL.createObjectURL(blob)
      const a    = document.createElement('a')
      a.href     = url
      a.download = `${invoiceNumber}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    },
  })
}
