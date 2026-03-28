import { memo } from 'react'
import { Send } from 'lucide-react'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { FollowUpResponse } from '@/types/followup.types'
import { timeAgo, dueDateLabel } from '@/utils/date'
import { formatCurrency } from '@/lib/format'

const CHANNEL_LABEL: Record<string, string> = {
  EMAIL: 'Email', SMS: 'SMS', WHATSAPP: 'WhatsApp',
}

const URGENCY = {
  OVERDUE:   { border: 'border-l-red-400',     dot: 'bg-red-500',   text: 'text-red-600 dark:text-red-400',     label: 'Overdue'   },
  DUE_TODAY: { border: 'border-l-amber-400',   dot: 'bg-amber-500', text: 'text-amber-600 dark:text-amber-400', label: 'Due Today' },
  NOT_DUE:   { border: 'border-l-blue-400',    dot: 'bg-blue-500',  text: 'text-blue-600 dark:text-blue-400',  label: 'Upcoming'  },
}

interface Props {
  invoice:       InvoiceResponse
  currency:      string
  customerName?: string
  lastFollowup?: FollowUpResponse
  onSend:        () => void
  canSend?:      boolean
}

const FollowupCard = memo(function FollowupCard({ invoice, currency, customerName, lastFollowup, onSend, canSend = true }: Props) {
  const u = URGENCY[invoice.timeStatus] ?? URGENCY.NOT_DUE

  return (
    <div className={`bg-white dark:bg-[#1B2838] rounded-xl border border-c-border border-l-4 ${u.border} p-4 space-y-3`}>

      {/* Client + urgency badge */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-sm font-bold text-[#0D1B2A] dark:text-white truncate">
            {customerName ?? 'Unknown client'}
          </p>
          <p className="text-xs text-c-muted mt-0.5 truncate">{invoice.invoiceNumber}</p>
        </div>
        <span className={`text-xs font-medium shrink-0 ${u.text}`}>{u.label}</span>
      </div>

      {/* Amount owed */}
      <div>
        <p className="text-[10px] text-c-muted uppercase tracking-wide mb-0.5">Remaining</p>
        <p className="text-lg font-bold tabular-nums text-[#0D1B2A] dark:text-white">
          {formatCurrency(invoice.remainingAmount, currency, { decimals: false })}
        </p>
      </div>

      {/* Due + last contacted */}
      <div className="space-y-1 text-xs text-c-muted">
        <p>
          Due{' '}
          <span className={
            invoice.timeStatus === 'OVERDUE'   ? 'font-semibold text-red-500' :
            invoice.timeStatus === 'DUE_TODAY' ? 'font-semibold text-amber-600 dark:text-amber-400' :
            'text-[#0D1B2A] dark:text-white'
          }>
            {dueDateLabel(invoice.dueDate)}
          </span>
        </p>
        <p>
          Last contacted:{' '}
          <span className="text-[#0D1B2A] dark:text-white">
            {lastFollowup
              ? `${CHANNEL_LABEL[lastFollowup.channel] ?? lastFollowup.channel} · ${timeAgo(lastFollowup.sentAt ?? lastFollowup.createdAt)}`
              : 'Never'
            }
          </span>
        </p>
      </div>

      {/* CTA */}
      {canSend ? (
        <button
          onClick={onSend}
          className="w-full flex items-center justify-center gap-1.5 py-2 rounded-lg text-sm font-semibold text-white transition-opacity hover:opacity-90"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          <Send size={13} strokeWidth={2.5} />
          Send Follow-up
        </button>
      ) : (
        <div className="w-full flex items-center justify-center gap-1.5 py-2 rounded-lg text-xs font-medium text-c-muted bg-[#F4F7F9] dark:bg-white/5 border border-c-border cursor-not-allowed">
          No client assigned
        </div>
      )}
    </div>
  )
})

export default FollowupCard
