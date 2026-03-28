import { AlertCircle, TrendingUp, CheckCircle2, Timer } from 'lucide-react'
import { formatCurrency, formatDate } from '@/lib/format'

function currencySymbol(currency: string): string {
  return (
    new Intl.NumberFormat('en', { style: 'currency', currency, minimumFractionDigits: 0, maximumFractionDigits: 0 })
      .formatToParts(0)
      .find((p) => p.type === 'currency')?.value ?? '$'
  )
}

// ---------------------------------------------------------------------------
// Card
// ---------------------------------------------------------------------------

interface CardProps {
  label:     string
  value:     string
  sub?:      string
  icon:      React.ReactNode
  iconBg:    string
  iconColor: string
  valueColor?: string
}

function KpiCard({ label, value, sub, icon, iconBg, iconColor, valueColor }: CardProps) {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 flex flex-col gap-4">
      <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${iconBg}`}>
        <span className={iconColor}>{icon}</span>
      </div>
      <div>
        <p className={`text-2xl font-bold tracking-tight ${valueColor ?? 'text-[#0D1B2A] dark:text-white'}`}>
          {value}
        </p>
        <p className="text-sm text-c-muted mt-0.5">{label}</p>
        {sub && <p className="text-xs text-c-muted/70 mt-1">{sub}</p>}
      </div>
    </div>
  )
}

function KpiCardSkeleton() {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 flex flex-col gap-4 animate-pulse">
      <div className="w-9 h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
      <div className="space-y-2">
        <div className="h-7 w-28 rounded bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-4 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// KpiStrip
// ---------------------------------------------------------------------------

interface Props {
  kpis: {
    totalUnpaid:          number
    totalUnpaidCount:     number
    overdueCount:         number
    pastDueAmount:        number
    collectedThisMonth:   number
    avgPaymentDelayDays:  number | null
    pendingApprovalsCount: number
  }
  currency:            string
  isConfirmationFlow:  boolean
  isLoading:           boolean
}

export default function KpiStrip({ kpis, currency, isConfirmationFlow, isLoading }: Props) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[...Array(4)].map((_, i) => <KpiCardSkeleton key={i} />)}
      </div>
    )
  }

  const cards: CardProps[] = [
    {
      label:     'Total Unpaid',
      value:     formatCurrency(kpis.totalUnpaid, currency, { decimals: false }),
      sub:       kpis.totalUnpaidCount === 1
        ? '1 invoice outstanding'
        : `${kpis.totalUnpaidCount} invoices outstanding`,
      icon:      <span className="text-sm font-bold leading-none">{currencySymbol(currency)}</span>,
      iconBg:    'bg-[#29B6F6]/10 dark:bg-[#29B6F6]/15',
      iconColor: 'text-[#29B6F6]',
    },
    {
      label:      'Past Due',
      value:      formatCurrency(kpis.pastDueAmount, currency, { decimals: false }),
      sub:        kpis.overdueCount === 1
        ? '1 invoice overdue'
        : `${kpis.overdueCount} invoices overdue`,
      icon:       <AlertCircle size={16} strokeWidth={2} />,
      iconBg:     'bg-red-50 dark:bg-red-500/10',
      iconColor:  'text-red-500',
      valueColor: kpis.overdueCount > 0 ? 'text-red-500' : undefined,
    },
    {
      label:      'Collected This Month',
      value:      formatCurrency(kpis.collectedThisMonth, currency, { decimals: false }),
      sub:        kpis.collectedThisMonth > 0 ? 'Payments received this month' : 'No payments received yet',
      icon:       <TrendingUp size={16} strokeWidth={2} />,
      iconBg:     'bg-green-50 dark:bg-green-500/10',
      iconColor:  'text-green-500',
      valueColor: kpis.collectedThisMonth > 0 ? 'text-green-600 dark:text-green-400' : undefined,
    },
    isConfirmationFlow
      ? {
          label:      'Pending Approvals',
          value:      String(kpis.pendingApprovalsCount),
          sub:        'Payment claims awaiting review',
          icon:       <CheckCircle2 size={16} strokeWidth={2} />,
          iconBg:     'bg-violet-50 dark:bg-violet-500/10',
          iconColor:  'text-violet-500',
          valueColor: kpis.pendingApprovalsCount > 0 ? 'text-violet-600 dark:text-violet-400' : undefined,
        }
      : {
          label:     'Avg Payment Delay',
          value:     kpis.avgPaymentDelayDays != null ? `${kpis.avgPaymentDelayDays}d` : '—',
          sub:       'Average days from issue to payment',
          icon:      <Timer size={16} strokeWidth={2} />,
          iconBg:    'bg-[#8A9BAE]/10 dark:bg-white/5',
          iconColor: 'text-c-muted',
        },
  ]

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => (
        <KpiCard key={card.label} {...card} />
      ))}
    </div>
  )
}
