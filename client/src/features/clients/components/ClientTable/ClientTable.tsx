import { useNavigate } from 'react-router-dom'
import { ChevronRight, Trash2, Users } from 'lucide-react'
import {
  computeAvgDelay,
  getRisk,
  RISK_META,
} from '../ClientMetricsStrip/ClientMetricsStrip'
import type { ViewMode } from '@/ui/components/ViewToggle'
import type { CustomerResponse } from '@/types/customer.types'
import type { InvoiceResponse } from '@/types/invoice.types'

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

interface ClientRow {
  customer:          CustomerResponse
  invoices:          InvoiceResponse[]
  currency:          string
  onDelete:          (c: CustomerResponse) => void
}

function MobileCard({ customer, invoices, currency, onDelete }: ClientRow) {
  const navigate = useNavigate()
  const outstanding = invoices.filter((i) => i.lifeCycleStatus !== 'PAID' && i.lifeCycleStatus !== 'CANCELLED').reduce((s, i) => s + i.remainingAmount, 0)
  const overdue     = invoices.filter((i) => i.timeStatus === 'OVERDUE' && i.lifeCycleStatus !== 'PAID').reduce((s, i) => s + i.remainingAmount, 0)
  const avgDelay    = computeAvgDelay(invoices)
  const risk        = getRisk(avgDelay, invoices.length > 0)
  const meta        = RISK_META[risk]

  return (
    <div
      onClick={() => navigate(`/clients/${customer.id}`)}
      className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-4 cursor-pointer hover:border-[#8A9BAE]/30 transition-colors"
    >
      <div className="flex items-start justify-between gap-2 mb-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white truncate">{customer.name}</p>
          {customer.companyName && <p className="text-xs text-c-muted truncate mt-0.5">{customer.companyName}</p>}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={(e) => { e.stopPropagation(); onDelete(customer) }}
            className="p-1 text-c-muted hover:text-red-500 transition-colors"
          >
            <Trash2 size={14} />
          </button>
        </div>
      </div>
      <div className="grid grid-cols-3 gap-2 text-center">
        <div>
          <p className="text-[10px] text-c-muted mb-0.5">Outstanding</p>
          <p className="text-xs font-semibold text-[#0D1B2A] dark:text-white tabular-nums">{fmt(outstanding, currency)}</p>
        </div>
        <div>
          <p className="text-[10px] text-c-muted mb-0.5">Overdue</p>
          <p className={`text-xs font-semibold tabular-nums ${overdue > 0 ? 'text-red-500' : 'text-c-muted'}`}>{overdue > 0 ? fmt(overdue, currency) : '—'}</p>
        </div>
        <div>
          <p className="text-[10px] text-c-muted mb-0.5">Risk</p>
          <span className={`text-xs font-medium ${meta.text}`}>{meta.label}</span>
        </div>
      </div>
    </div>
  )
}

interface Props {
  customers:          CustomerResponse[]
  invoicesByCustomer: Record<string, InvoiceResponse[]>
  currency:           string
  isLoading:          boolean
  view?:              ViewMode
  onDelete:           (c: CustomerResponse) => void
}

export default function ClientTable({ customers, invoicesByCustomer, currency, isLoading, view = 'list', onDelete }: Props) {
  const navigate = useNavigate()

  if (isLoading) {
    return (
      <div className="space-y-3 animate-pulse">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-20 rounded-xl bg-[#F4F7F9] dark:bg-white/10 md:h-12" />
        ))}
      </div>
    )
  }

  if (customers.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[30vh] gap-3 text-center">
        <div className="w-12 h-12 rounded-full bg-[#29B6F6]/10 flex items-center justify-center">
          <Users size={20} className="text-[#29B6F6]" />
        </div>
        <div>
          <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">No clients yet</p>
          <p className="text-sm text-c-muted mt-0.5">Add your first client to get started.</p>
        </div>
      </div>
    )
  }

  const showCards = view === 'grid2' || view === 'grid3'
  const cardCols  = view === 'grid3'
    ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3'
    : 'grid grid-cols-1 sm:grid-cols-2 gap-3'

  return (
    <>
      {/* Card grid — shown when view is grid2 or grid3, or on mobile regardless */}
      <div className={showCards ? cardCols : 'grid grid-cols-1 sm:grid-cols-2 gap-3 md:hidden'}>
        {customers.map((c) => (
          <MobileCard
            key={c.id}
            customer={c}
            invoices={invoicesByCustomer[c.id] ?? []}
            currency={currency}
            onDelete={onDelete}
          />
        ))}
      </div>

      {/* Table — shown only in list view, hidden on mobile */}
      <div className={showCards ? 'hidden' : 'hidden md:block bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden'}>
        <table className="w-full">
          <thead>
            <tr className="border-b border-c-border">
              <th className="py-3 px-4 text-xs font-semibold uppercase tracking-wide text-c-muted text-left">Client</th>
              <th className="py-3 px-4 text-xs font-semibold uppercase tracking-wide text-c-muted text-right">Outstanding</th>
              <th className="py-3 px-4 text-xs font-semibold uppercase tracking-wide text-c-muted text-right">Overdue</th>
              <th className="py-3 px-4 text-xs font-semibold uppercase tracking-wide text-c-muted text-right">Risk</th>
              <th className="py-3 px-4" />
            </tr>
          </thead>
          <tbody>
            {customers.map((c) => {
              const invs        = invoicesByCustomer[c.id] ?? []
              const outstanding = invs.filter((i) => i.lifeCycleStatus !== 'PAID' && i.lifeCycleStatus !== 'CANCELLED').reduce((s, i) => s + i.remainingAmount, 0)
              const overdue     = invs.filter((i) => i.timeStatus === 'OVERDUE' && i.lifeCycleStatus !== 'PAID').reduce((s, i) => s + i.remainingAmount, 0)
              const avgDelay    = computeAvgDelay(invs)
              const risk        = getRisk(avgDelay, invs.length > 0)
              const meta        = RISK_META[risk]

              return (
                <tr
                  key={c.id}
                  onClick={() => navigate(`/clients/${c.id}`)}
                  className="group border-b border-c-border last:border-0 hover:bg-[#F4F7F9]/50 dark:hover:bg-white/5 transition-colors cursor-pointer"
                >
                  <td className="py-3 px-4">
                    <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">{c.name}</p>
                    {c.companyName && <p className="text-xs text-c-muted mt-0.5">{c.companyName}</p>}
                  </td>
                  <td className="py-3 px-4 text-right text-sm tabular-nums text-[#0D1B2A] dark:text-white">
                    {fmt(outstanding, currency)}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <span className={`text-sm tabular-nums font-medium ${overdue > 0 ? 'text-red-500' : 'text-c-muted'}`}>
                      {overdue > 0 ? fmt(overdue, currency) : '—'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <span className={`text-sm font-medium ${meta.text}`}>{meta.label}</span>
                  </td>
                  <td className="py-3 px-4">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={(e) => { e.stopPropagation(); onDelete(c) }}
                        className="p-1 text-c-muted opacity-0 group-hover:opacity-100 hover:text-red-500 transition-all"
                      >
                        <Trash2 size={14} />
                      </button>
                      <ChevronRight size={16} className="text-c-muted" />
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </>
  )
}
