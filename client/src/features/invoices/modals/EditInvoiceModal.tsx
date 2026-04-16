import { useState } from 'react'
import { X } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { useUpdateInvoice } from '../hooks/useInvoiceMutations'
import InvoiceForm, { type InvoiceFormValues } from '../components/InvoiceForm/InvoiceForm'
import type { InvoiceResponse } from '@/types/invoice.types'

interface Props {
  invoice: InvoiceResponse
  onClose: () => void
}

const btnPrimary = [
  'px-4 py-2 rounded-lg text-sm font-semibold text-white',
  'hover:opacity-90 transition-opacity disabled:opacity-50',
].join(' ')

// "GST (18%)" → 18,  "VAT (20.5%)" → 20.5,  anything else → 0
function parsePct(label: string): number {
  const m = label.match(/\((\d+(?:\.\d+)?)%\)$/)
  return m ? parseFloat(m[1]) : 0
}

// "GST (18%)" → "GST",  "Tax" → "Tax"
function parseLabel(label: string): string {
  return label.replace(/\s*\(\d+(?:\.\d+)?%\)$/, '').trim()
}

export default function EditInvoiceModal({ invoice, onClose }: Props) {
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')
  const update   = useUpdateInvoice(invoice.id)

  // Reverse-engineer form values from the stored invoice
  const firstTax          = invoice.taxLines?.[0]
  const subtotal          = invoice.subtotal ?? 0
  const storedDiscount    = invoice.discountAmount ?? 0
  const discountPct       = subtotal > 0 && storedDiscount > 0
    ? Math.round((storedDiscount / subtotal) * 10000) / 100
    : 0

  const [values, setValues] = useState<InvoiceFormValues>({
    invoiceNumber:      invoice.invoiceNumber,
    customerId:         invoice.customerId ?? '',
    dueDate:            invoice.dueDate    ?? '',
    taxLabel:           firstTax ? parseLabel(firstTax.label) : '',
    taxPercentage:      firstTax ? parsePct(firstTax.label)   : 0,
    discountPercentage: discountPct,
    items:              invoice.items.map((it) => ({
      description: it.description,
      quantity:    it.quantity,
      unitPrice:   it.unitPrice,
    })),
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

    const newSubtotal     = Math.round(values.items.reduce((s, it) => s + it.quantity * it.unitPrice, 0))
    const taxAmount       = values.taxPercentage > 0 ? Math.round(newSubtotal * values.taxPercentage / 100) : 0
    const discountAmount  = values.discountPercentage > 0 ? Math.round(newSubtotal * values.discountPercentage / 100) : 0
    const taxLabel        = values.taxLabel.trim() || 'Tax'

    try {
      await update.mutateAsync({
        invoiceNumber:  values.invoiceNumber.trim(),
        customerId:     values.customerId || undefined,
        dueDate:        values.dueDate    || undefined,
        taxLines:       taxAmount > 0
          ? [{ label: `${taxLabel} (${values.taxPercentage}%)`, amount: taxAmount }]
          : [],
        discountAmount: discountAmount > 0 ? discountAmount : 0,
        items:          values.items,
      })
      onClose()
    } catch {
      setError('Failed to save changes. Please try again.')
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end sm:items-center sm:justify-center sm:p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />

      <form
        onSubmit={handleSubmit}
        className="relative w-full sm:max-w-2xl bg-white dark:bg-[#1B2838] rounded-t-2xl sm:rounded-2xl shadow-xl overflow-hidden max-h-[95dvh] sm:max-h-[90vh] flex flex-col"
      >
        {/* Drag handle — mobile only */}
        <div className="sm:hidden flex justify-center pt-3 pb-1 shrink-0">
          <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
        </div>

        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-c-border shrink-0">
          <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">
            Edit Invoice · {invoice.invoiceNumber}
          </h2>
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
          <InvoiceForm currency={currency} initial={values} onChange={setValues} />
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
            disabled={update.isPending}
            className={btnPrimary}
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            {update.isPending ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </form>
    </div>
  )
}
