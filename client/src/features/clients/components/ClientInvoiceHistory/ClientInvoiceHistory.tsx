import { useNavigate } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import type { InvoiceResponse } from '@/types/invoice.types'
import { formatCurrency, formatDate } from '@/lib/format'
import { getInvoiceStatusDisplay } from '@/features/invoices/components/InvoiceStatusBadge/InvoiceStatusBadge'

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
        const { timeMeta, lcLabel, lcCls } = getInvoiceStatusDisplay(inv.lifeCycleStatus, inv.timeStatus)
        const label = timeMeta ? timeMeta.label : lcLabel
        const text  = timeMeta ? timeMeta.cls   : lcCls
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

            {/* Due date */}
            <span className="text-xs text-c-muted shrink-0 tabular-nums hidden sm:block w-20">
              {formatDate(inv.dueDate)}
            </span>

            {/* Amount */}
            <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white shrink-0 tabular-nums w-24">
              {formatCurrency(inv.totalAmount, currency, { decimals: false })}
            </span>

            {/* Status */}
            <span className={`text-xs font-medium shrink-0 ${text}`}>{label}</span>

            <ArrowRight size={13} className="text-c-muted shrink-0 ml-auto" />
          </li>
        )
      })}
    </ul>
  )
}
