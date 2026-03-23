import { useNavigate } from 'react-router-dom'
import { ArrowRight, Trash2 } from 'lucide-react'
import type { InvoiceResponse, LifeCycleStatus, TimeStatus } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fmt(amount: number, currency: string) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency, maximumFractionDigits: 0,
  }).format(amount)
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

// ---------------------------------------------------------------------------
// Status chip — combines lifecycle + time context
// ---------------------------------------------------------------------------

const LIFECYCLE_STYLE: Record<LifeCycleStatus, string> = {
  DRAFT:          'bg-[#8A9BAE]/10 text-[#8A9BAE]',
  ISSUED:         'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400',
  PARTIALLY_PAID: 'bg-[#29B6F6]/10 text-[#29B6F6] dark:text-[#29B6F6]',
  PAID:           'bg-green-50 text-green-600 dark:bg-green-500/15 dark:text-green-400',
  CANCELLED:      'bg-red-50 text-red-500 dark:bg-red-500/10 dark:text-red-400',
}

function StatusChip({ lifecycle, time }: { lifecycle: LifeCycleStatus; time: TimeStatus }) {
  const overdue   = time === 'OVERDUE' && lifecycle !== 'PAID' && lifecycle !== 'CANCELLED' && lifecycle !== 'DRAFT'
  const dueToday  = time === 'DUE_TODAY' && lifecycle !== 'PAID' && lifecycle !== 'CANCELLED'

  if (overdue) {
    return <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400">Overdue</span>
  }
  if (dueToday) {
    return <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400">Due Today</span>
  }
  return (
    <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${LIFECYCLE_STYLE[lifecycle]}`}>
      {lifecycle.replace('_', ' ')}
    </span>
  )
}

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------

function RowSkeleton() {
  return (
    <tr className="animate-pulse border-b border-[#F4F7F9] dark:border-white/10">
      {[140, 120, 80, 90, 80, 80, 40].map((w, i) => (
        <td key={i} className="px-4 py-3.5">
          <div className={`h-4 rounded bg-[#F4F7F9] dark:bg-white/10`} style={{ width: w }} />
        </td>
      ))}
    </tr>
  )
}

// ---------------------------------------------------------------------------
// InvoiceTable
// ---------------------------------------------------------------------------

interface Props {
  invoices:    InvoiceResponse[]
  customerMap: Record<string, CustomerResponse>
  currency:    string
  isLoading:   boolean
  onDelete?:   (invoice: InvoiceResponse) => void
}

export default function InvoiceTable({ invoices, customerMap, currency, isLoading, onDelete }: Props) {
  const navigate = useNavigate()

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#F4F7F9] dark:border-white/10">
              {['Invoice #', 'Customer', 'Status', 'Due Date', 'Total', 'Remaining', ''].map((h) => (
                <th
                  key={h}
                  className="px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wide text-[#8A9BAE]"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              [...Array(5)].map((_, i) => <RowSkeleton key={i} />)
            ) : invoices.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-16 text-center text-sm text-[#8A9BAE]">
                  No invoices found
                </td>
              </tr>
            ) : (
              invoices.map((inv) => {
                const customer = inv.customerId ? customerMap[inv.customerId] : null
                const isDraft  = inv.lifeCycleStatus === 'DRAFT'
                return (
                  <tr
                    key={inv.id}
                    className="border-b border-[#F4F7F9] dark:border-white/10 hover:bg-[#F4F7F9] dark:hover:bg-[#243447] cursor-pointer transition-colors"
                    onClick={() => navigate(`/invoices/${inv.id}`)}
                  >
                    <td className="px-4 py-3.5 font-semibold text-[#0D1B2A] dark:text-white">
                      {inv.invoiceNumber}
                    </td>
                    <td className="px-4 py-3.5 text-[#0D1B2A] dark:text-white">
                      {customer?.name ?? customer?.companyName ?? <span className="text-[#8A9BAE]">—</span>}
                    </td>
                    <td className="px-4 py-3.5">
                      <StatusChip lifecycle={inv.lifeCycleStatus} time={inv.timeStatus} />
                    </td>
                    <td className="px-4 py-3.5 text-[#8A9BAE] tabular-nums">
                      {fmtDate(inv.dueDate)}
                    </td>
                    <td className="px-4 py-3.5 font-medium text-[#0D1B2A] dark:text-white tabular-nums">
                      {fmt(inv.totalAmount, currency)}
                    </td>
                    <td className="px-4 py-3.5 tabular-nums">
                      <span className={inv.remainingAmount > 0 && inv.lifeCycleStatus !== 'CANCELLED' ? 'text-red-500 font-medium' : 'text-[#8A9BAE]'}>
                        {fmt(inv.remainingAmount, currency)}
                      </span>
                    </td>
                    <td
                      className="px-4 py-3.5"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {isDraft && onDelete && (
                        <button
                          onClick={() => onDelete(inv)}
                          className="p-1.5 rounded-lg text-[#8A9BAE] hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
                          aria-label="Delete invoice"
                        >
                          <Trash2 size={14} strokeWidth={2} />
                        </button>
                      )}
                      {!isDraft && (
                        <ArrowRight size={14} className="text-[#8A9BAE]" />
                      )}
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
