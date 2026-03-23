import { useNavigate } from 'react-router-dom'
import { ExternalLink } from 'lucide-react'
import type { InvoiceResponse, LifeCycleStatus, TimeStatus } from '@/types/invoice.types'

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

const LIFECYCLE_STYLE: Record<LifeCycleStatus, string> = {
  DRAFT:          'bg-[#8A9BAE]/10 text-[#8A9BAE]',
  ISSUED:         'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400',
  PARTIALLY_PAID: 'bg-[#29B6F6]/10 text-[#29B6F6]',
  PAID:           'bg-green-50 text-green-600 dark:bg-green-500/15 dark:text-green-400',
  CANCELLED:      'bg-red-50 text-red-500 dark:bg-red-500/10 dark:text-red-400',
}

function statusLabel(lc: LifeCycleStatus, ts: TimeStatus) {
  if (ts === 'OVERDUE'   && lc !== 'PAID' && lc !== 'CANCELLED') return { label: 'Overdue',   chip: 'bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400' }
  if (ts === 'DUE_TODAY' && lc !== 'PAID' && lc !== 'CANCELLED') return { label: 'Due Today', chip: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400' }
  return { label: lc.replace('_', ' '), chip: LIFECYCLE_STYLE[lc] }
}

interface Props {
  invoices:  InvoiceResponse[]
  currency:  string
  isLoading: boolean
}

export default function ClientInvoiceHistory({ invoices, currency, isLoading }: Props) {
  const navigate = useNavigate()

  if (isLoading) {
    return (
      <div className="space-y-2 animate-pulse">
        {[...Array(3)].map((_, i) => <div key={i} className="h-12 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />)}
      </div>
    )
  }

  if (invoices.length === 0) {
    return <p className="text-sm text-[#8A9BAE] py-4 text-center">No invoices for this client yet.</p>
  }

  return (
    <>
      {/* Mobile: stacked */}
      <div className="space-y-2 sm:hidden">
        {invoices.map((inv) => {
          const { label, chip } = statusLabel(inv.lifeCycleStatus, inv.timeStatus)
          return (
            <div
              key={inv.id}
              onClick={() => navigate(`/invoices/${inv.id}`)}
              className="flex items-center justify-between gap-3 p-3 rounded-lg border border-[#F4F7F9] dark:border-white/10 cursor-pointer hover:bg-[#F4F7F9]/50 dark:hover:bg-white/5 transition-colors"
            >
              <div className="min-w-0">
                <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">{inv.invoiceNumber}</p>
                <p className="text-xs text-[#8A9BAE]">Due {fmtDate(inv.dueDate)}</p>
              </div>
              <div className="text-right shrink-0">
                <p className="text-sm font-semibold tabular-nums text-[#0D1B2A] dark:text-white">{fmt(inv.totalAmount, currency)}</p>
                <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded-full ${chip}`}>{label}</span>
              </div>
            </div>
          )
        })}
      </div>

      {/* Desktop: table */}
      <div className="hidden sm:block">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#F4F7F9] dark:border-white/10">
              {['Invoice #', 'Amount', 'Due Date', 'Status', ''].map((h) => (
                <th key={h} className={`py-2.5 text-xs font-semibold uppercase tracking-wide text-[#8A9BAE] ${h === '' ? 'text-right' : 'text-left'}`}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {invoices.map((inv) => {
              const { label, chip } = statusLabel(inv.lifeCycleStatus, inv.timeStatus)
              return (
                <tr
                  key={inv.id}
                  onClick={() => navigate(`/invoices/${inv.id}`)}
                  className="border-b border-[#F4F7F9] dark:border-white/10 last:border-0 hover:bg-[#F4F7F9]/40 dark:hover:bg-white/5 transition-colors cursor-pointer"
                >
                  <td className="py-3 text-sm font-medium text-[#0D1B2A] dark:text-white pr-4">{inv.invoiceNumber}</td>
                  <td className="py-3 text-sm tabular-nums text-[#0D1B2A] dark:text-white pr-4">{fmt(inv.totalAmount, currency)}</td>
                  <td className="py-3 text-sm text-[#8A9BAE] pr-4">{fmtDate(inv.dueDate)}</td>
                  <td className="py-3 pr-4">
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${chip}`}>{label}</span>
                  </td>
                  <td className="py-3 text-right">
                    <ExternalLink size={14} className="text-[#8A9BAE] inline" />
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
