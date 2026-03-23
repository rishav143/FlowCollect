import type { CustomerResponse } from '@/types/customer.types'
import type { InvoiceResponse } from '@/types/invoice.types'

// ---------------------------------------------------------------------------
// Risk classification (UI_LAYOUT.md spec)
// avg delay < 5 days  → Good
// 5–14 days           → Slow
// > 14 days           → Risk
// Approximated from overdue invoice days on frontend (no payment history API in v1)
// ---------------------------------------------------------------------------

export type RiskLabel = 'GOOD' | 'SLOW' | 'RISK' | 'NONE'

export function computeAvgDelay(invoices: InvoiceResponse[]): number {
  const today = Date.now()
  const overdue = invoices.filter((i) =>
    i.timeStatus === 'OVERDUE' &&
    i.lifeCycleStatus !== 'PAID' &&
    i.lifeCycleStatus !== 'CANCELLED' &&
    i.dueDate,
  )
  if (overdue.length === 0) return 0
  const totalDays = overdue.reduce((sum, inv) => {
    const daysDiff = Math.floor((today - new Date(inv.dueDate!).getTime()) / 86400_000)
    return sum + Math.max(0, daysDiff)
  }, 0)
  return Math.round(totalDays / overdue.length)
}

export function getRisk(avgDelay: number, hasInvoices: boolean): RiskLabel {
  if (!hasInvoices) return 'NONE'
  if (avgDelay < 5)  return 'GOOD'
  if (avgDelay <= 14) return 'SLOW'
  return 'RISK'
}

export const RISK_META: Record<RiskLabel, { label: string; dot: string; chip: string }> = {
  GOOD: { label: 'Good',      dot: '🟢', chip: 'bg-green-50 text-green-600 dark:bg-green-500/15 dark:text-green-400' },
  SLOW: { label: 'Slow',      dot: '🟡', chip: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400' },
  RISK: { label: 'High Risk', dot: '🔴', chip: 'bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400' },
  NONE: { label: 'No Data',   dot: '⚪', chip: 'bg-[#F4F7F9] text-[#8A9BAE] dark:bg-white/10 dark:text-[#8A9BAE]' },
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

interface Props {
  customers: CustomerResponse[]
  invoicesByCustomer: Record<string, InvoiceResponse[]>
  isLoading: boolean
}

export default function ClientMetricsStrip({ customers, invoicesByCustomer, isLoading }: Props) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 animate-pulse">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-20 rounded-xl bg-[#F4F7F9] dark:bg-white/10" />
        ))}
      </div>
    )
  }

  const total = customers.length

  let highRisk = 0
  let goodPayers = 0
  let totalDelay = 0
  let clientsWithInvoices = 0

  for (const c of customers) {
    const invs = invoicesByCustomer[c.id] ?? []
    const delay = computeAvgDelay(invs)
    const risk  = getRisk(delay, invs.length > 0)
    if (risk === 'RISK') highRisk++
    if (risk === 'GOOD') goodPayers++
    if (invs.length > 0) { totalDelay += delay; clientsWithInvoices++ }
  }

  const avgDelay = clientsWithInvoices > 0 ? Math.round(totalDelay / clientsWithInvoices) : 0

  const cards = [
    { label: 'Total Clients',     value: String(total),             sub: 'active',           color: 'text-[#29B6F6]' },
    { label: 'High Risk',         value: String(highRisk),          sub: 'avg delay > 14d',  color: 'text-red-500'   },
    { label: 'Good Payers',       value: String(goodPayers),        sub: 'avg delay < 5d',   color: 'text-green-500' },
    { label: 'Avg Payment Delay', value: `${avgDelay}d`,            sub: 'across all clients',color: 'text-[#8A9BAE]' },
  ]

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
      {cards.map((c) => (
        <div
          key={c.label}
          className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 p-4"
        >
          <p className="text-xs font-semibold uppercase tracking-wide text-[#8A9BAE] mb-1">{c.label}</p>
          <p className={`text-2xl font-bold tabular-nums ${c.color}`}>{c.value}</p>
          <p className="text-xs text-[#8A9BAE] mt-0.5">{c.sub}</p>
        </div>
      ))}
    </div>
  )
}
