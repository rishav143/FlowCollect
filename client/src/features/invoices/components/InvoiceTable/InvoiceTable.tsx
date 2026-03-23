import { useNavigate } from 'react-router-dom'
import { ArrowRight, Trash2 } from 'lucide-react'
import type { InvoiceResponse, LifeCycleStatus, TimeStatus } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

function fmt(amount: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount)
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

function getStatus(lc: LifeCycleStatus, ts: TimeStatus): { label: string; chip: string } {
  if (ts === 'OVERDUE'   && lc !== 'PAID' && lc !== 'CANCELLED' && lc !== 'DRAFT')
    return { label: 'Overdue',   chip: 'bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400' }
  if (ts === 'DUE_TODAY' && lc !== 'PAID' && lc !== 'CANCELLED')
    return { label: 'Due Today', chip: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400' }
  const MAP: Record<LifeCycleStatus, { label: string; chip: string }> = {
    DRAFT:          { label: 'Draft',     chip: 'bg-[#8A9BAE]/10 text-[#8A9BAE]' },
    ISSUED:         { label: 'Sent',      chip: 'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400' },
    PARTIALLY_PAID: { label: 'Partial',   chip: 'bg-[#29B6F6]/10 text-[#29B6F6]' },
    PAID:           { label: 'Paid',      chip: 'bg-green-50 text-green-600 dark:bg-green-500/15 dark:text-green-400' },
    CANCELLED:      { label: 'Cancelled', chip: 'bg-red-50 text-red-500 dark:bg-red-500/10 dark:text-red-400' },
  }
  return MAP[lc]
}

function RowSkeleton() {
  return (
    <tr className="animate-pulse border-b border-[#F4F7F9] dark:border-white/10">
      {[160, 80, 100, 100, 40].map((w, i) => (
        <td key={i} className="px-4 py-3.5">
          <div className="h-4 rounded bg-[#F4F7F9] dark:bg-white/10" style={{ width: w }} />
        </td>
      ))}
    </tr>
  )
}

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
              {['Client', 'Status', 'Due Date', 'Amount', ''].map((h) => (
                <th
                  key={h}
                  className={`px-4 py-3 text-[11px] font-semibold uppercase tracking-wide text-[#8A9BAE] ${h === 'Amount' ? 'text-right' : 'text-left'}`}
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
                <td colSpan={5} className="py-16 text-center text-sm text-[#8A9BAE]">
                  No invoices found
                </td>
              </tr>
            ) : (
              invoices.map((inv) => {
                const customer  = inv.customerId ? customerMap[inv.customerId] : null
                const isDraft   = inv.lifeCycleStatus === 'DRAFT'
                const isPaid    = inv.lifeCycleStatus === 'PAID'
                const isPartial = inv.lifeCycleStatus === 'PARTIALLY_PAID'
                const { label, chip } = getStatus(inv.lifeCycleStatus, inv.timeStatus)

                return (
                  <tr
                    key={inv.id}
                    className="border-b border-[#F4F7F9] dark:border-white/10 last:border-0 hover:bg-[#F4F7F9] dark:hover:bg-[#243447] cursor-pointer transition-colors"
                    onClick={() => navigate(`/invoices/${inv.id}`)}
                  >
                    {/* Client + invoice # */}
                    <td className="px-4 py-3.5">
                      <p className="font-semibold text-[#0D1B2A] dark:text-white">
                        {customer?.name ?? customer?.companyName ?? <span className="text-[#8A9BAE]">No client</span>}
                      </p>
                      <p className="text-xs text-[#8A9BAE] mt-0.5">{inv.invoiceNumber}</p>
                    </td>

                    {/* Status */}
                    <td className="px-4 py-3.5">
                      <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${chip}`}>
                        {label}
                      </span>
                    </td>

                    {/* Due date */}
                    <td className={`px-4 py-3.5 tabular-nums text-sm ${inv.timeStatus === 'OVERDUE' && !isPaid ? 'text-red-500 font-medium' : 'text-[#8A9BAE]'}`}>
                      {isPaid ? '—' : fmtDate(inv.dueDate)}
                    </td>

                    {/* Amount — total + remaining hint */}
                    <td className="px-4 py-3.5 text-right">
                      <p className={`font-bold tabular-nums ${isPaid ? 'text-green-600 dark:text-green-400' : 'text-[#0D1B2A] dark:text-white'}`}>
                        {fmt(inv.totalAmount, currency)}
                      </p>
                      {isPartial && (
                        <p className={`text-xs tabular-nums mt-0.5 ${inv.timeStatus === 'OVERDUE' ? 'text-red-500' : 'text-[#8A9BAE]'}`}>
                          {fmt(inv.remainingAmount, currency)} owed
                        </p>
                      )}
                    </td>

                    {/* Action */}
                    <td
                      className="px-4 py-3.5"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {isDraft && onDelete ? (
                        <button
                          onClick={() => onDelete(inv)}
                          className="p-1.5 rounded-lg text-[#8A9BAE] hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
                        >
                          <Trash2 size={14} strokeWidth={2} />
                        </button>
                      ) : (
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
