import { useNavigate } from 'react-router-dom'
import { ArrowRight, Trash2, FileText } from 'lucide-react'
import InvoiceStatusBadge from '../InvoiceStatusBadge/InvoiceStatusBadge'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'
import { formatCurrency, formatDate } from '@/lib/format'
import { useAuthStore } from '@/store/auth.store'
// ---------------------------------------------------------------------------
// Aging helper
// ---------------------------------------------------------------------------

function agingLabel(inv: InvoiceResponse, orgTimezone?: string): { label: string; overdue: boolean } {
  if (inv.lifeCycleStatus === 'PAID' || inv.lifeCycleStatus === 'CANCELLED' || inv.lifeCycleStatus === 'DRAFT') {
    return { label: '—', overdue: false }
  }
  if (!inv.dueDate) return { label: '—', overdue: false }

  const tz = orgTimezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone
  // Get today as YYYY-MM-DD in the org timezone (en-CA locale gives ISO date format)
  const todayStr = new Date().toLocaleDateString('en-CA', { timeZone: tz })
  const dueDateStr = inv.dueDate.split('T')[0]
  // Build local Date objects (not UTC) to compare at day granularity
  const [ty, tm, td] = todayStr.split('-').map(Number)
  const [dy, dm, dd] = dueDateStr.split('-').map(Number)
  const days = Math.round(
    (new Date(ty, tm - 1, td).getTime() - new Date(dy, dm - 1, dd).getTime()) / 86_400_000
  )

  if (days < 0)   return { label: `Due in ${Math.abs(days)}d`, overdue: false }
  if (days === 0) return { label: 'Due today', overdue: false }
  return { label: `${days}d overdue`, overdue: true }
}

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------

function RowSkeleton() {
  return (
    <tr className="animate-pulse border-b border-c-border">
      {[120, 140, 90, 80, 90, 80, 32].map((w, i) => (
        <td key={i} className="px-4 py-3.5">
          <div className="h-4 rounded bg-[#F4F7F9] dark:bg-white/10" style={{ width: w }} />
        </td>
      ))}
    </tr>
  )
}

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface Props {
  invoices:    InvoiceResponse[]
  customerMap: Record<string, CustomerResponse>
  currency:    string
  isLoading:   boolean
  onDelete?:   (invoice: InvoiceResponse) => void
}

// ---------------------------------------------------------------------------
// Table
// ---------------------------------------------------------------------------

const HEADERS = ['Invoice #', 'Client', 'Status', 'Aging', 'Due Date', 'Amount', '']

export default function InvoiceTable({ invoices, customerMap, currency, isLoading, onDelete }: Props) {
  const navigate     = useNavigate()
  const orgTimezone  = useAuthStore((s) => s.org?.timezone)

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-c-border bg-[#F9FAFB] dark:bg-white/[0.02]">
              {HEADERS.map((h) => (
                <th
                  key={h}
                  className={[
                    'px-4 py-3 text-[11px] font-semibold uppercase tracking-wide text-c-muted',
                    h === 'Amount' ? 'text-right' : 'text-left',
                  ].join(' ')}
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
                <td colSpan={HEADERS.length}>
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
                const customer    = inv.customerId ? customerMap[inv.customerId] : null
                const isDraft     = inv.lifeCycleStatus === 'DRAFT'
                const isPaid      = inv.lifeCycleStatus === 'PAID'
                const isCancelled = inv.lifeCycleStatus === 'CANCELLED'
                const isPartial   = inv.lifeCycleStatus === 'PARTIALLY_PAID'
                const { label: agingText, overdue } = agingLabel(inv, orgTimezone)

                return (
                  <tr
                    key={inv.id}
                    onClick={() => navigate(`/invoices/${inv.id}`)}
                    className={[
                      'border-b border-c-border last:border-0 cursor-pointer transition-colors',
                      overdue
                        ? 'bg-red-50/40 dark:bg-red-500/[0.04] hover:bg-red-50/70 dark:hover:bg-red-500/[0.06]'
                        : 'hover:bg-[#F4F7F9] dark:hover:bg-[#243447]',
                    ].join(' ')}
                  >
                    {/* Invoice # */}
                    <td className="px-4 py-3.5">
                      <p className="font-medium text-[#0D1B2A] dark:text-white tabular-nums">
                        {inv.invoiceNumber}
                      </p>
                      <p className="text-xs text-c-muted mt-0.5 tabular-nums">
                        {formatDate(inv.createdAt)}
                      </p>
                    </td>

                    {/* Client */}
                    <td className="px-4 py-3.5">
                      <p className="font-medium text-[#0D1B2A] dark:text-white truncate max-w-[160px]">
                        {customer?.name ?? customer?.companyName ?? (
                          <span className="text-c-muted italic">No client</span>
                        )}
                      </p>
                    </td>

                    {/* Status */}
                    <td className="px-4 py-3.5">
                      <InvoiceStatusBadge lifecycle={inv.lifeCycleStatus} time={inv.timeStatus} />
                    </td>

                    {/* Aging */}
                    <td className="px-4 py-3.5">
                      <span className={[
                        'text-sm tabular-nums',
                        overdue ? 'text-red-500 dark:text-red-400 font-medium' : 'text-c-muted',
                      ].join(' ')}>
                        {agingText}
                      </span>
                    </td>

                    {/* Due Date */}
                    <td className="px-4 py-3.5 tabular-nums text-sm text-c-muted">
                      {isPaid || isCancelled || isDraft ? '—' : formatDate(inv.dueDate)}
                    </td>

                    {/* Amount */}
                    <td className="px-4 py-3.5 text-right">
                      <p className={[
                        'font-bold tabular-nums',
                        isCancelled
                          ? 'line-through text-c-muted'
                          : isPaid
                            ? 'text-green-600 dark:text-green-400'
                            : 'text-[#0D1B2A] dark:text-white',
                      ].join(' ')}>
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
