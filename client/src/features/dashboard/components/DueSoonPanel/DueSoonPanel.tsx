import { useNavigate } from 'react-router-dom'
import { ArrowRight, CalendarClock } from 'lucide-react'
import type { InvoiceResponse } from '@/types/invoice.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fmt(amount: number, currency: string) {
  return new Intl.NumberFormat('en-IN', {
    style:                 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(amount)
}

function daysUntil(dueDate: string): number {
  const due   = new Date(dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.max(0, Math.ceil((due.getTime() - today.getTime()) / 86_400_000))
}

// Dot + text urgency indicator — no background shading
function urgencyBadge(days: number) {
  if (days === 0) return { dot: 'bg-red-500',   text: 'text-red-600 dark:text-red-400',     label: 'Due today' }
  if (days <= 3)  return { dot: 'bg-amber-500', text: 'text-amber-600 dark:text-amber-400', label: `${days}d`  }
  if (days <= 7)  return { dot: 'bg-blue-500',  text: 'text-blue-600 dark:text-blue-400',   label: `${days}d`  }
  return           { dot: 'bg-[#8A9BAE]',       text: 'text-c-muted',                        label: `${days}d`  }
}

// ---------------------------------------------------------------------------
// Skeleton row
// ---------------------------------------------------------------------------

function RowSkeleton({ isLast }: { isLast: boolean }) {
  return (
    <div className={`flex items-center gap-3 px-5 py-3.5 animate-pulse ${!isLast ? 'border-b border-c-border' : ''}`}>
      <div className="h-4 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="h-5 w-14 rounded-full bg-[#F4F7F9] dark:bg-white/10" />
      <div className="flex-1" />
      <div className="h-4 w-16 rounded bg-[#F4F7F9] dark:bg-white/10" />
    </div>
  )
}

// ---------------------------------------------------------------------------
// DueSoonPanel
// ---------------------------------------------------------------------------

/**
 * DueSoonPanel
 *
 * Shows invoices due in the next 14 days, sorted by nearest due date.
 * Urgency is colour-coded so users can act on the most time-sensitive
 * invoices first and send reminders before they become overdue.
 */
export default function DueSoonPanel({
  invoices,
  currency,
  isLoading,
}: {
  invoices:  InvoiceResponse[]
  currency:  string
  isLoading: boolean
}) {
  const navigate = useNavigate()

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">

      {/* Header */}
      <div className="flex items-center gap-2 px-5 py-4 border-b border-c-border">
        <CalendarClock size={15} className="text-amber-500 shrink-0" />
        <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Due Soon</h2>
        {!isLoading && invoices.length > 0 && (
          <span className="ml-auto text-xs font-medium text-c-muted">
            {invoices.length} upcoming
          </span>
        )}
      </div>

      {/* Body */}
      {isLoading ? (
        <>{[...Array(3)].map((_, i) => <RowSkeleton key={i} isLast={i === 2} />)}</>
      ) : invoices.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-10 gap-1">
          <p className="text-sm text-c-muted">No invoices due in the next 14 days</p>
          <p className="text-xs text-c-muted/70">You're ahead of schedule</p>
        </div>
      ) : (
        <ul>
          {invoices.map((inv, idx) => {
            const days  = inv.dueDate ? daysUntil(inv.dueDate) : 0
            const badge = urgencyBadge(days)
            return (
              <li
                key={inv.id}
                className={[
                  'flex items-center gap-3 px-5 py-3.5 cursor-pointer',
                  'hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors',
                  idx < invoices.length - 1 ? 'border-b border-c-border' : '',
                ].join(' ')}
                onClick={() => navigate(`/invoices/${inv.id}`)}
              >
                <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white w-24 shrink-0">
                  {inv.invoiceNumber}
                </span>

                <span className="inline-flex items-center gap-1.5 shrink-0">
                  <span className={`w-1.5 h-1.5 rounded-full ${badge.dot}`} />
                  <span className={`text-xs font-medium ${badge.text}`}>{badge.label}</span>
                </span>

                <span className="flex-1" />

                <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
                  {fmt(inv.remainingAmount, currency)}
                </span>

                <ArrowRight size={14} className="text-c-muted shrink-0" />
              </li>
            )
          })}
        </ul>
      )}

    </div>
  )
}
