import { DollarSign, AlertCircle, TrendingUp, Clock, CheckCircle2 } from 'lucide-react'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fmt(amount: number, currency: string) {
  return new Intl.NumberFormat('en-IN', {
    style:                 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(amount)
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
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 p-5 flex flex-col gap-4">
      <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${iconBg}`}>
        <span className={iconColor}>{icon}</span>
      </div>
      <div>
        <p className={`text-2xl font-bold tracking-tight ${valueColor ?? 'text-[#0D1B2A] dark:text-white'}`}>
          {value}
        </p>
        <p className="text-sm text-[#8A9BAE] mt-0.5">{label}</p>
        {sub && <p className="text-xs text-[#8A9BAE]/70 mt-1">{sub}</p>}
      </div>
    </div>
  )
}

function KpiCardSkeleton() {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 p-5 flex flex-col gap-4 animate-pulse">
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
    outstanding:          number
    overdueCount:         number
    collectedThisMonth:   number
    draftCount:           number
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
      label:      'Outstanding',
      value:      fmt(kpis.outstanding, currency),
      sub:        'Total remaining balance',
      icon:       <DollarSign size={16} strokeWidth={2} />,
      iconBg:     'bg-[#29B6F6]/10 dark:bg-[#29B6F6]/15',
      iconColor:  'text-[#29B6F6]',
    },
    {
      label:      'Overdue',
      value:      String(kpis.overdueCount),
      sub:        kpis.overdueCount === 1 ? '1 invoice past due date' : `${kpis.overdueCount} invoices past due date`,
      icon:       <AlertCircle size={16} strokeWidth={2} />,
      iconBg:     'bg-red-50 dark:bg-red-500/10',
      iconColor:  'text-red-500',
      valueColor: kpis.overdueCount > 0 ? 'text-red-500' : undefined,
    },
    {
      label:      'Collected this month',
      value:      fmt(kpis.collectedThisMonth, currency),
      sub:        'Payments received',
      icon:       <TrendingUp size={16} strokeWidth={2} />,
      iconBg:     'bg-green-50 dark:bg-green-500/10',
      iconColor:  'text-green-500',
      valueColor: kpis.collectedThisMonth > 0 ? 'text-green-600 dark:text-green-400' : undefined,
    },
    isConfirmationFlow
      ? {
          label:      'Pending approvals',
          value:      String(kpis.pendingApprovalsCount),
          sub:        'Payment claims awaiting review',
          icon:       <CheckCircle2 size={16} strokeWidth={2} />,
          iconBg:     'bg-amber-50 dark:bg-amber-500/10',
          iconColor:  'text-amber-500',
          valueColor: kpis.pendingApprovalsCount > 0 ? 'text-amber-600 dark:text-amber-400' : undefined,
        }
      : {
          label:      'Drafts',
          value:      String(kpis.draftCount),
          sub:        'Unsent invoices',
          icon:       <Clock size={16} strokeWidth={2} />,
          iconBg:     'bg-[#8A9BAE]/10 dark:bg-white/5',
          iconColor:  'text-[#8A9BAE]',
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
