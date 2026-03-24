import { useNavigate } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import type { InvoiceResponse, LifeCycleStatus, TimeStatus } from '@/types/invoice.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

// ---------------------------------------------------------------------------
// Status
// ---------------------------------------------------------------------------

const LIFECYCLE_MAP: Record<LifeCycleStatus, { label: string; dot: string; text: string }> = {
  DRAFT:          { label: 'Draft',     dot: 'bg-[#8A9BAE]', text: 'text-c-muted'                          },
  ISSUED:         { label: 'Sent',      dot: 'bg-blue-500',  text: 'text-blue-600 dark:text-blue-400'      },
  PARTIALLY_PAID: { label: 'Partial',   dot: 'bg-[#29B6F6]', text: 'text-[#29B6F6] dark:text-[#4FC3F7]'   },
  PAID:           { label: 'Paid',      dot: 'bg-green-500', text: 'text-green-600 dark:text-green-400'    },
  CANCELLED:      { label: 'Cancelled', dot: 'bg-[#8A9BAE]', text: 'text-c-muted'                          },
}

function getStatus(lc: LifeCycleStatus, ts: TimeStatus) {
  if (ts === 'OVERDUE'   && lc !== 'PAID' && lc !== 'CANCELLED')
    return { label: 'Overdue',   dot: 'bg-red-500',   text: 'text-red-600 dark:text-red-400'   }
  if (ts === 'DUE_TODAY' && lc !== 'PAID' && lc !== 'CANCELLED' && lc !== 'PARTIALLY_PAID')
    return { label: 'Due Today', dot: 'bg-amber-500', text: 'text-amber-600 dark:text-amber-400' }
  return LIFECYCLE_MAP[lc]
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function ClientInvoiceHistory({
  invoices,
  currency,
  isLoading,
}: {
  invoices:  InvoiceResponse[]
  currency:  string
  isLoading: boolean
}) {
  const navigate = useNavigate()

  if (isLoading) {
    return (
      <div className="space-y-2 animate-pulse">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-11 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
        ))}
      </div>
    )
  }

  if (invoices.length === 0) {
    return (
      <p className="text-sm text-c-muted py-6 text-center">
        No invoices for this client yet.
      </p>
    )
  }

  return (
    <ul>
      {invoices.map((inv, idx) => {
        const { label, dot, text } = getStatus(inv.lifeCycleStatus, inv.timeStatus)
        const isLast = idx === invoices.length - 1
        return (
          <li
            key={inv.id}
            onClick={() => navigate(`/invoices/${inv.id}`)}
            className={[
              'flex items-center gap-3 py-3 cursor-pointer',
              'hover:bg-[#F4F7F9]/50 dark:hover:bg-white/5 transition-colors rounded-lg px-2 -mx-2',
              !isLast ? 'border-b border-c-border' : '',
            ].join(' ')}
          >
            {/* Invoice # */}
            <span className="text-sm font-medium text-[#0D1B2A] dark:text-white w-20 shrink-0 tabular-nums">
              {inv.invoiceNumber}
            </span>

            {/* Status */}
            <span className="inline-flex items-center gap-1.5 shrink-0">
              <span className={`w-1.5 h-1.5 rounded-full ${dot}`} />
              <span className={`text-xs font-medium ${text}`}>{label}</span>
            </span>

            <span className="flex-1" />

            {/* Due date — hidden on very small screens */}
            <span className="text-xs text-c-muted hidden xs:block sm:block tabular-nums">
              {fmtDate(inv.dueDate)}
            </span>

            {/* Amount */}
            <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
              {fmt(inv.totalAmount, currency)}
            </span>

            <ArrowRight size={13} className="text-c-muted shrink-0" />
          </li>
        )
      })}
    </ul>
  )
}
