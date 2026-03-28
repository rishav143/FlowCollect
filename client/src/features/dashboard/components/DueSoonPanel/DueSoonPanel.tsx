import { useNavigate } from 'react-router-dom'
import { ArrowRight, CalendarClock, Bell } from 'lucide-react'
import type { InvoiceResponse } from '@/types/invoice.types'
import { formatCurrency } from '@/lib/format'

function daysUntil(dueDate: string): number {
  const due   = new Date(dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.max(0, Math.ceil((due.getTime() - today.getTime()) / 86_400_000))
}

function urgencyLabel(days: number): { label: string; text: string } {
  if (days === 0) return { label: 'Due today', text: 'text-red-600 dark:text-red-400' }
  return           { label: `${days}d`,        text: 'text-c-muted'                   }
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

export default function DueSoonPanel({
  invoices,
  currency,
  customerMap,
  isLoading,
  onFollowup,
}: {
  invoices:    InvoiceResponse[]
  currency:    string
  customerMap: Record<string, string>
  isLoading:   boolean
  onFollowup?: (inv: InvoiceResponse) => void
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
            const badge = urgencyLabel(days)
            return (
              <li
                key={inv.id}
                className={[
                  'group cursor-pointer',
                  'hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors',
                  idx < invoices.length - 1 ? 'border-b border-c-border' : '',
                ].join(' ')}
                onClick={() => navigate(`/invoices/${inv.id}`)}
              >
                {/* ── Mobile layout (< sm) — two rows ── */}
                <div className="sm:hidden px-5 py-3">
                  <div className="flex items-center gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{inv.invoiceNumber}</p>
                      {inv.customerId && customerMap[inv.customerId] && (
                        <p className="text-xs text-c-muted truncate">{customerMap[inv.customerId]}</p>
                      )}
                    </div>
                    <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums shrink-0">
                      {formatCurrency(inv.remainingAmount, currency, { decimals: false })}
                    </span>
                    <ArrowRight size={14} className="text-c-muted shrink-0" />
                  </div>
                  <div className="flex items-center justify-between mt-1.5">
                    <span className={`text-xs ${badge.text}`}>{badge.label}</span>
                    {onFollowup && (
                      <button
                        onClick={(e) => { e.stopPropagation(); onFollowup(inv) }}
                        title="Send follow-up"
                        className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-[#29B6F6] hover:bg-[#29B6F6]/10 transition-colors"
                      >
                        <Bell size={12} strokeWidth={2.5} />
                      </button>
                    )}
                  </div>
                </div>

                {/* ── Desktop/tablet layout (sm+) — original single row ── */}
                <div className="hidden sm:flex items-center gap-3 px-5 py-3">
                  <div className="w-28 shrink-0 min-w-0">
                    <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{inv.invoiceNumber}</p>
                    {inv.customerId && customerMap[inv.customerId] && (
                      <p className="text-xs text-c-muted truncate">{customerMap[inv.customerId]}</p>
                    )}
                  </div>
                  <span className={`text-xs font-medium shrink-0 ${badge.text}`}>{badge.label}</span>
                  <span className="flex-1" />
                  {onFollowup && (
                    <button
                      onClick={(e) => { e.stopPropagation(); onFollowup(inv) }}
                      title="Send follow-up"
                      className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-[#29B6F6] opacity-0 group-hover:opacity-100 hover:bg-[#29B6F6]/10 transition-all shrink-0"
                    >
                      <Bell size={12} strokeWidth={2.5} />
                      Follow-up
                    </button>
                  )}
                  <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
                    {formatCurrency(inv.remainingAmount, currency, { decimals: false })}
                  </span>
                  <ArrowRight size={14} className="text-c-muted shrink-0" />
                </div>
              </li>
            )
          })}
        </ul>
      )}

    </div>
  )
}
