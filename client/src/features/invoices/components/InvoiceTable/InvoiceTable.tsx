import { useNavigate } from 'react-router-dom'
import { ArrowRight, Trash2, FileText } from 'lucide-react'
import InvoiceStatusBadge from '../InvoiceStatusBadge/InvoiceStatusBadge'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'
import { formatCurrency, formatDate } from '@/lib/format'

function RowSkeleton() {
  return (
    <tr className="animate-pulse border-b border-c-border">
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
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-c-border">
              {['Client', 'Status', 'Due Date', 'Amount', ''].map((h) => (
                <th
                  key={h}
                  className={`px-4 py-3 text-[11px] font-semibold uppercase tracking-wide text-c-muted ${h === 'Amount' ? 'text-right' : 'text-left'}`}
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
                <td colSpan={5}>
                  <div className="flex flex-col items-center justify-center py-16 gap-3 text-center">
                    <div className="w-12 h-12 rounded-full bg-[#F4F7F9] dark:bg-white/10 flex items-center justify-center">
                      <FileText size={20} className="text-c-muted" strokeWidth={1.5} />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">No invoices found</p>
                      <p className="text-xs text-c-muted mt-0.5">Try adjusting your filters.</p>
                    </div>
                  </div>
                </td>
              </tr>
            ) : (
              invoices.map((inv) => {
                const customer  = inv.customerId ? customerMap[inv.customerId] : null
                const isDraft   = inv.lifeCycleStatus === 'DRAFT'
                const isPaid    = inv.lifeCycleStatus === 'PAID'
                const isPartial = inv.lifeCycleStatus === 'PARTIALLY_PAID'

                return (
                  <tr
                    key={inv.id}
                    className="border-b border-c-border last:border-0 hover:bg-[#F4F7F9] dark:hover:bg-[#243447] cursor-pointer transition-colors"
                    onClick={() => navigate(`/invoices/${inv.id}`)}
                  >
                    {/* Client + invoice # */}
                    <td className="px-4 py-3.5">
                      <p className="font-semibold text-[#0D1B2A] dark:text-white">
                        {customer?.name ?? customer?.companyName ?? <span className="text-c-muted">No client</span>}
                      </p>
                      <p className="text-xs text-c-muted mt-0.5">{inv.invoiceNumber}</p>
                    </td>

                    {/* Status */}
                    <td className="px-4 py-3.5">
                      <InvoiceStatusBadge lifecycle={inv.lifeCycleStatus} time={inv.timeStatus} className="text-sm" />
                    </td>

                    {/* Due date */}
                    <td className="px-4 py-3.5 tabular-nums text-sm text-c-muted">
                      {isPaid ? '—' : formatDate(inv.dueDate)}
                    </td>

                    {/* Amount */}
                    <td className="px-4 py-3.5 text-right">
                      <p className={`font-bold tabular-nums ${isPaid ? 'text-green-600 dark:text-green-400' : 'text-[#0D1B2A] dark:text-white'}`}>
                        {formatCurrency(inv.totalAmount, currency, { decimals: false })}
                      </p>
                      {isPartial && (
                        <p className="text-xs tabular-nums mt-0.5 text-c-muted">
                          {formatCurrency(inv.remainingAmount, currency, { decimals: false })} remaining
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
                          className="p-1.5 rounded-lg text-c-muted hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
                        >
                          <Trash2 size={14} strokeWidth={2} />
                        </button>
                      ) : (
                        <ArrowRight size={14} className="text-c-muted" />
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
