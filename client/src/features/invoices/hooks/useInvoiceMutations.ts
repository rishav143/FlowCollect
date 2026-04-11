import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import {
  createInvoice,
  updateInvoice,
  deleteInvoice,
  issueInvoice,
  cancelInvoice,
  downloadInvoicePdf,
  type CreateInvoiceBody,
  type UpdateInvoiceBody,
} from '@/api/invoice.api'

export function useCreateInvoice() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (body: CreateInvoiceBody) => createInvoice(orgId, body),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['invoices', orgId] })
      qc.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })
    },
  })
}

export function useUpdateInvoice(invoiceId: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (body: UpdateInvoiceBody) => updateInvoice(orgId, invoiceId, body),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['invoices', orgId] })
      qc.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })
    },
  })
}

export function useDeleteInvoice() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => deleteInvoice(orgId, id),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['invoices', orgId] })
      qc.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })
    },
  })
}

export function useIssueInvoice(invoiceId: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (issueDate?: string) => issueInvoice(orgId, invoiceId, issueDate),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['invoices', orgId] })
      qc.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })
    },
  })
}

export function useCancelInvoice(invoiceId: string) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: () => cancelInvoice(orgId, invoiceId),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['invoices', orgId] })
      qc.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })
    },
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
      a.style.display = 'none'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    },
    onError: async (err: unknown) => {
      const res = (err as { response?: { data?: unknown } })?.response
      let message = 'Failed to download PDF.'
      if (res?.data instanceof Blob) {
        try {
          const text = await (res.data as Blob).text()
          const json = JSON.parse(text)
          message = json.message ?? json.error ?? text
        } catch {
          message = await (res.data as Blob).text().catch(() => message)
        }
      } else if (typeof res?.data === 'object' && res?.data !== null) {
        message = (res.data as { message?: string }).message ?? message
      }
      console.error('[PDF download error]', message)
      alert(message)
    },
  })
}
