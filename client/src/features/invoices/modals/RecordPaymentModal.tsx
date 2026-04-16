import { useState } from 'react'
import { X } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { recordPayment } from '@/api/payment.api'
import type { PaymentMode } from '@/types/payment.types'
import { formatCurrency } from '@/lib/format'
import Select from '@/components/ui/Select'

const MODES = [
  { value: 'BANK_TRANSFER', label: 'Bank / Wire Transfer' },
  { value: 'CARD',          label: 'Card' },
  { value: 'UPI',           label: 'UPI' },
  { value: 'CASH',          label: 'Cash' },
  { value: 'CHEQUE',        label: 'Cheque' },
]

const inputCls = [
  'w-full px-3 py-2 text-sm bg-[#F4F7F9] dark:bg-[#243447] rounded-lg',
  'border border-transparent focus:border-[#8A9BAE]/40 focus:outline-none',
  'text-[#0D1B2A] dark:text-white placeholder:text-c-muted transition-colors',
].join(' ')

const labelCls = 'block text-xs font-semibold text-c-muted uppercase tracking-wide mb-1.5'

interface Props {
  invoiceId:       string
  remainingAmount: number
  currency:        string
  onClose:         () => void
}

export default function RecordPaymentModal({ invoiceId, remainingAmount, currency, onClose }: Props) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  const [amount,      setAmount]      = useState(Math.round(remainingAmount))
  const [mode,        setMode]        = useState<PaymentMode>('BANK_TRANSFER')
  const [referenceId, setReferenceId] = useState('')
  const [notes,       setNotes]       = useState('')
  const [error,       setError]       = useState<string | null>(null)


  const mutation = useMutation({
    mutationFn: () => recordPayment(orgId, invoiceId, { amount, mode, referenceId: referenceId || undefined, notes: notes || undefined }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['invoices', orgId] })
      qc.invalidateQueries({ queryKey: ['payments', orgId, invoiceId] })
      qc.invalidateQueries({ queryKey: ['followups', orgId, invoiceId] })
      onClose()
    },
    onError: () => setError('Failed to record payment. Please try again.'),
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (amount <= 0) { setError('Amount must be greater than 0.'); return }
    mutation.mutate()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <form
        onSubmit={handleSubmit}
        className="relative w-full max-w-md bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl overflow-hidden"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-c-border">
          <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Record Payment</h2>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="px-6 py-5 space-y-4">
          <p className="text-sm text-c-muted">Remaining: <span className="font-semibold text-[#0D1B2A] dark:text-white">{formatCurrency(remainingAmount, currency, { decimals: false })}</span></p>

          <div>
            <label className={labelCls}>Amount *</label>
            <input type="number" min={1} step={1} value={amount} onChange={(e) => setAmount(Math.round(parseFloat(e.target.value) || 0))} className={inputCls} />
          </div>

          <div>
            <label className={labelCls}>Payment Mode</label>
            <Select
              value={mode}
              onChange={(v) => setMode(v as PaymentMode)}
              options={MODES}
            />
          </div>

          <div>
            <label className={labelCls}>Reference ID</label>
            <input type="text" placeholder="UTR / Transaction ID" value={referenceId} onChange={(e) => setReferenceId(e.target.value)} className={inputCls} />
          </div>

          <div>
            <label className={labelCls}>Notes</label>
            <input type="text" placeholder="Optional note" value={notes} onChange={(e) => setNotes(e.target.value)} className={inputCls} />
          </div>

          {error && <p className="text-sm text-red-500">{error}</p>}
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 px-6 py-4 border-t border-c-border">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
            Cancel
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity disabled:opacity-50"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            {mutation.isPending ? 'Recording…' : 'Record Payment'}
          </button>
        </div>
      </form>
    </div>
  )
}
