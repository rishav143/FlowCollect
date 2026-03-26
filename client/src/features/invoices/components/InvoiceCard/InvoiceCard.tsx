import { memo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Trash2 } from 'lucide-react'
import type { InvoiceResponse, LifeCycleStatus, TimeStatus } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

type StatusInfo = { label: string; dot: string; text: string }

function getStatus(lc: LifeCycleStatus, ts: TimeStatus): StatusInfo {
  if (ts === 'OVERDUE'   && lc !== 'PAID' && lc !== 'CANCELLED' && lc !== 'DRAFT')
    return { label: 'Overdue',   dot: 'bg-red-500',   text: 'text-red-600 dark:text-red-400' }
  if (ts === 'DUE_TODAY' && lc !== 'PAID' && lc !== 'CANCELLED' && lc !== 'PARTIALLY_PAID')
    return { label: 'Due Today', dot: 'bg-amber-500', text: 'text-amber-600 dark:text-amber-400' }
  const MAP: Record<LifeCycleStatus, StatusInfo> = {
    DRAFT:          { label: 'Draft',     dot: 'bg-[#8A9BAE]', text: 'text-c-muted' },
    ISSUED:         { label: 'Issued',     dot: 'bg-blue-500',  text: 'text-blue-600 dark:text-blue-400' },
    PARTIALLY_PAID: { label: 'Partial',   dot: 'bg-[#29B6F6]', text: 'text-[#29B6F6]' },
    PAID:           { label: 'Paid',      dot: 'bg-green-500', text: 'text-green-600 dark:text-green-400' },
    CANCELLED:      { label: 'Cancelled', dot: 'bg-[#8A9BAE]', text: 'text-c-muted' },
  }
  return MAP[lc]
}

interface Props {
  invoice:  InvoiceResponse
  customer: CustomerResponse | undefined
  currency: string
  onDelete: (inv: InvoiceResponse) => void
}

const InvoiceCard = memo(function InvoiceCard({ invoice, customer, currency, onDelete }: Props) {
  const navigate = useNavigate()
  const { label, dot, text } = getStatus(invoice.lifeCycleStatus, invoice.timeStatus)
  const isPaid  = invoice.lifeCycleStatus === 'PAID'
  const isDraft = invoice.lifeCycleStatus === 'DRAFT'

  return (
    <div
      onClick={() => navigate(`/invoices/${invoice.id}`)}
      className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-4 cursor-pointer hover:border-[#8A9BAE]/30 dark:hover:border-white/20 transition-colors space-y-3"
    >
      {/* Client + status */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-sm font-bold text-[#0D1B2A] dark:text-white truncate">
            {customer?.name ?? customer?.companyName ?? 'No client'}
          </p>
          <p className="text-xs text-c-muted truncate mt-0.5">{invoice.invoiceNumber}</p>
        </div>
        <span className={`text-sm font-medium shrink-0 ${text}`}>{label}</span>
      </div>

      {/* Amount */}
      <p className={`text-xl font-bold tabular-nums ${isPaid ? 'text-green-600 dark:text-green-400' : 'text-[#0D1B2A] dark:text-white'}`}>
        {fmt(invoice.totalAmount, currency)}
      </p>

      {/* Due date + delete */}
      <div className="flex items-center justify-between pt-1 border-t border-c-border">
        <p className="text-xs text-c-muted">
          {isPaid ? 'Paid' : `Due ${fmtDate(invoice.dueDate)}`}
        </p>
        {isDraft && (
          <button
            onClick={(e) => { e.stopPropagation(); onDelete(invoice) }}
            className="p-1 text-c-muted hover:text-red-500 transition-colors"
          >
            <Trash2 size={13} />
          </button>
        )}
      </div>
    </div>
  )
})

export default InvoiceCard
