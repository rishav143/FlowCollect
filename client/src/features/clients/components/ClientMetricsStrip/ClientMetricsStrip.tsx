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

export const RISK_META: Record<RiskLabel, { label: string; dot: string; text: string }> = {
  GOOD: { label: 'Good',      dot: 'bg-green-500', text: 'text-green-600 dark:text-green-400' },
  SLOW: { label: 'Slow',      dot: 'bg-amber-500', text: 'text-amber-600 dark:text-amber-400' },
  RISK: { label: 'High Risk', dot: 'bg-red-500',   text: 'text-red-600 dark:text-red-400' },
  NONE: { label: 'No Data',   dot: 'bg-[#8A9BAE]', text: 'text-c-muted' },
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

interface Props {
  customers: CustomerResponse[]
  invoicesByCustomer: Record<string, InvoiceResponse[]>
  currency: string
  isLoading: boolean
}

export default function ClientMetricsStrip({ customers, invoicesByCustomer, currency, isLoading }: Props) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 animate-pulse">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-20 rounded-xl bg-[#F4F7F9] dark:bg-white/10" />
        ))}
      </div>
    )
  }

  let totalOutstanding = 0
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
    for (const inv of invs) {
      if (inv.lifeCycleStatus !== 'PAID' && inv.lifeCycleStatus !== 'CANCELLED') {
        totalOutstanding += inv.remainingAmount
      }
    }
  }

  const avgDelay = clientsWithInvoices > 0 ? Math.round(totalDelay / clientsWithInvoices) : 0

  function fmt(n: number) {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
  }

  const cards = [
    { label: 'Outstanding',       value: fmt(totalOutstanding), sub: 'total unpaid',       color: totalOutstanding > 0 ? 'text-[#0D1B2A] dark:text-white' : 'text-green-600 dark:text-green-400' },
    { label: 'High Risk',         value: String(highRisk),      sub: 'avg delay > 14d',    color: highRisk > 0 ? 'text-red-500' : 'text-[#0D1B2A] dark:text-white' },
    { label: 'Good Payers',       value: String(goodPayers),    sub: 'avg delay < 5d',     color: goodPayers > 0 ? 'text-green-600 dark:text-green-400' : 'text-[#0D1B2A] dark:text-white' },
    { label: 'Avg Payment Delay', value: `${avgDelay}d`,        sub: 'across all clients', color: 'text-[#0D1B2A] dark:text-white' },
  ]

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
      {cards.map((c) => (
        <div key={c.label} className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-4">
          <p className={`text-2xl font-bold tabular-nums ${c.color}`}>{c.value}</p>
          <p className="text-sm text-c-muted mt-0.5">{c.label}</p>
          <p className="text-xs text-c-muted/70 mt-0.5">{c.sub}</p>
        </div>
      ))}
    </div>
  )
}
