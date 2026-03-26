import { memo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Trash2 } from 'lucide-react'
import InvoiceStatusBadge from '../InvoiceStatusBadge/InvoiceStatusBadge'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

interface Props {
  invoice:  InvoiceResponse
  customer: CustomerResponse | undefined
  currency: string
  onDelete: (inv: InvoiceResponse) => void
}

const InvoiceCard = memo(function InvoiceCard({ invoice, customer, currency, onDelete }: Props) {
  const navigate = useNavigate()
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
        <InvoiceStatusBadge lifecycle={invoice.lifeCycleStatus} time={invoice.timeStatus} className="text-sm shrink-0" />
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
