import { useState } from 'react'
import { X } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { recordPayment } from '@/api/payment.api'
import type { PaymentMode } from '@/types/payment.types'

const MODES: { value: PaymentMode; label: string }[] = [
  { value: 'UPI',           label: 'UPI' },
  { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
  { value: 'CASH',          label: 'Cash' },
  { value: 'CARD',          label: 'Card' },
  { value: 'CHEQUE',        label: 'Cheque' },
]

const inputCls = [
  'w-full px-3 py-2 text-sm bg-[#F4F7F9] dark:bg-[#243447] rounded-lg',
  'border border-transparent focus:border-[#8A9BAE]/40 focus:outline-none',
  'text-[#0D1B2A] dark:text-white placeholder:text-[#8A9BAE] transition-colors',
].join(' ')

const labelCls = 'block text-xs font-semibold text-[#8A9BAE] uppercase tracking-wide mb-1.5'

interface Props {
  invoiceId:       string
  remainingAmount: number
  currency:        string
  onClose:         () => void
}

export default function RecordPaymentModal({ invoiceId, remainingAmount, currency, onClose }: Props) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()

  const [amount,      setAmount]      = useState(remainingAmount)
  const [mode,        setMode]        = useState<PaymentMode>('UPI')
  const [referenceId, setReferenceId] = useState('')
  const [notes,       setNotes]       = useState('')
  const [error,       setError]       = useState<string | null>(null)

  const fmt = (n: number) =>
    new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)

  const mutation = useMutation({
    mutationFn: () => recordPayment(orgId, invoiceId, { amount, mode, referenceId: referenceId || undefined, notes: notes || undefined }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['invoices', orgId] })
      qc.invalidateQueries({ queryKey: ['payments', orgId, invoiceId] })
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
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#F4F7F9] dark:border-white/10">
          <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Record Payment</h2>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg text-[#8A9BAE] hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="px-6 py-5 space-y-4">
          <p className="text-sm text-[#8A9BAE]">Remaining: <span className="font-semibold text-[#0D1B2A] dark:text-white">{fmt(remainingAmount)}</span></p>

          <div>
            <label className={labelCls}>Amount *</label>
            <input type="number" min={0.01} step={0.01} value={amount} onChange={(e) => setAmount(parseFloat(e.target.value) || 0)} className={inputCls} />
          </div>

          <div>
            <label className={labelCls}>Payment Mode</label>
            <select value={mode} onChange={(e) => setMode(e.target.value as PaymentMode)} className={inputCls}>
              {MODES.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
            </select>
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
        <div className="flex justify-end gap-3 px-6 py-4 border-t border-[#F4F7F9] dark:border-white/10">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg text-sm font-medium text-[#8A9BAE] hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
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
