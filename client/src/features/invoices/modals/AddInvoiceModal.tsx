import { useState, useMemo } from 'react'
import { X } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { useCreateInvoice } from '../hooks/useInvoiceMutations'
import { listInvoices } from '@/api/invoice.api'
import InvoiceForm, { type InvoiceFormValues } from '../components/InvoiceForm/InvoiceForm'

interface Props {
  onClose: () => void
}

const btnPrimary = [
  'px-4 py-2 rounded-lg text-sm font-semibold text-white',
  'hover:opacity-90 transition-opacity disabled:opacity-50',
].join(' ')

function useNextInvoiceNumber(orgId: string): { suggested: string; isLoading: boolean } {
  const year = new Date().getFullYear()

  const { data, isLoading } = useQuery({
    queryKey: ['invoices', orgId, 'sequence', year],
    queryFn:  () => listInvoices(orgId, { invoiceNumber: `INV-${year}`, size: 500 }),
    enabled:  !!orgId,
    staleTime: 0,
  })

  const suggested = useMemo(() => {
    const pattern = new RegExp(`^INV-${year}-(\\d+)$`, 'i')
    let max = 0
    for (const inv of data?.content ?? []) {
      const m = inv.invoiceNumber.match(pattern)
      if (m) max = Math.max(max, parseInt(m[1], 10))
    }
    return `INV-${year}-${String(max + 1).padStart(3, '0')}`
  }, [data, year])

  return { suggested, isLoading }
}

export default function AddInvoiceModal({ onClose }: Props) {
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')
  const create   = useCreateInvoice()

  const { suggested, isLoading: loadingNumber } = useNextInvoiceNumber(orgId)

  const [values, setValues] = useState<InvoiceFormValues>({
    invoiceNumber:      '',
    customerId:         '',
    dueDate:            '',
    taxLabel:           '',
    taxPercentage:      0,
    discountPercentage: 0,
    items:              [{ description: '', quantity: 1, unitPrice: 0 }],
  })
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)

    if (!values.invoiceNumber.trim()) {
      setError('Invoice number is required.')
      return
    }
    if (values.items.some((it) => !it.description.trim() || it.quantity < 1)) {
      setError('All line items need a description and a quantity ≥ 1.')
      return
    }

    const subtotal       = Math.round(values.items.reduce((s, it) => s + it.quantity * it.unitPrice, 0))
    const taxAmount      = values.taxPercentage > 0 ? Math.round(subtotal * values.taxPercentage / 100) : 0
    const discountAmount = values.discountPercentage > 0 ? Math.round(subtotal * values.discountPercentage / 100) : 0
    const taxLabel       = values.taxLabel.trim() || 'Tax'

    try {
      await create.mutateAsync({
        invoiceNumber:  values.invoiceNumber.trim(),
        customerId:     values.customerId || undefined,
        dueDate:        values.dueDate    || undefined,
        taxLines:       taxAmount > 0 ? [{ label: `${taxLabel} (${values.taxPercentage}%)`, amount: taxAmount }] : undefined,
        discountAmount: discountAmount > 0 ? discountAmount : undefined,
        items:          values.items,
      })
      onClose()
    } catch {
      setError('Failed to create invoice. Please try again.')
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />

      <form
        onSubmit={handleSubmit}
        className="relative w-full max-w-2xl bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl overflow-hidden max-h-[90vh] flex flex-col"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-c-border shrink-0">
          <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">New Invoice</h2>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-6 py-5">
          {loadingNumber ? (
            <div className="space-y-4 animate-pulse">
              <div className="h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
              <div className="h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
              <div className="h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
            </div>
          ) : (
            // key forces re-mount with the correct initial value once suggestion is ready
            <InvoiceForm
              key={suggested}
              currency={currency}
              initial={{ invoiceNumber: suggested }}
              onChange={setValues}
            />
          )}
          {error && <p className="mt-3 text-sm text-red-500">{error}</p>}
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 px-6 py-4 border-t border-c-border shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-sm font-medium text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={create.isPending || loadingNumber}
            className={btnPrimary}
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            {create.isPending ? 'Creating…' : 'Create Invoice'}
          </button>
        </div>
      </form>
    </div>
  )
}
