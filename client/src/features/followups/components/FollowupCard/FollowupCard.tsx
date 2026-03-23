import { Send } from 'lucide-react'
import type { InvoiceResponse } from '@/types/invoice.types'

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

const TIME_CHIP: Record<string, string> = {
  OVERDUE:   'bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400',
  DUE_TODAY: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400',
  NOT_DUE:   'bg-[#F4F7F9] text-[#8A9BAE] dark:bg-white/10 dark:text-[#8A9BAE]',
}

const TIME_LABEL: Record<string, string> = {
  OVERDUE:   'Overdue',
  DUE_TODAY: 'Due Today',
  NOT_DUE:   'Upcoming',
}

const LIFECYCLE_CHIP: Record<string, string> = {
  ISSUED:         'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400',
  PARTIALLY_PAID: 'bg-[#29B6F6]/10 text-[#29B6F6]',
}

interface Props {
  invoice:    InvoiceResponse
  currency:   string
  customerName?: string
  onSend:     () => void
}

export default function FollowupCard({ invoice, currency, customerName, onSend }: Props) {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 p-4 space-y-3">
      {/* Top row */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white truncate">
            {invoice.invoiceNumber}
          </p>
          {customerName && (
            <p className="text-xs text-[#8A9BAE] truncate mt-0.5">{customerName}</p>
          )}
        </div>
        <div className="flex items-center gap-1.5 shrink-0">
          <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${LIFECYCLE_CHIP[invoice.lifeCycleStatus] ?? ''}`}>
            {invoice.lifeCycleStatus.replace('_', ' ')}
          </span>
          <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${TIME_CHIP[invoice.timeStatus] ?? ''}`}>
            {TIME_LABEL[invoice.timeStatus] ?? invoice.timeStatus}
          </span>
        </div>
      </div>

      {/* Amount row */}
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-[#8A9BAE]">Due {fmtDate(invoice.dueDate)}</p>
        </div>
        <div className="text-right">
          <p className="text-sm font-bold text-red-500 tabular-nums">
            {fmt(invoice.remainingAmount, currency)} remaining
          </p>
          <p className="text-xs text-[#8A9BAE] tabular-nums">
            of {fmt(invoice.totalAmount, currency)}
          </p>
        </div>
      </div>

      {/* Send button */}
      <button
        onClick={onSend}
        className="w-full flex items-center justify-center gap-1.5 py-2 rounded-lg text-sm font-semibold text-white transition-opacity hover:opacity-90"
        style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
      >
        <Send size={13} strokeWidth={2.5} />
        Send Follow-up
      </button>
    </div>
  )
}
