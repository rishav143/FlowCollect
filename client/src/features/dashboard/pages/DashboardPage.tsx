import { useNavigate } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { useDashboard } from '../hooks/useDashboard'
import KpiStrip     from '../components/KpiStrip/KpiStrip'
import ActionTable  from '../components/ActionTable/ActionTable'
import DueSoonPanel from '../components/DueSoonPanel/DueSoonPanel'
import type { InvoiceResponse, LifeCycleStatus } from '@/types/invoice.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function greeting(): string {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

function todayLabel(): string {
  return new Date().toLocaleDateString('en-IN', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
  })
}

function fmt(amount: number, currency: string) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency, maximumFractionDigits: 0,
  }).format(amount)
}

function timeAgo(iso: string): string {
  const mins = Math.floor((Date.now() - new Date(iso).getTime()) / 60_000)
  if (mins < 60)  return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24)   return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  if (days === 1) return 'Yesterday'
  return `${days}d ago`
}

// ---------------------------------------------------------------------------
// Health pill — at-a-glance status shown in the header
// ---------------------------------------------------------------------------

function HealthPill({
  overdueCount,
  pastDueAmount,
  pendingApprovalsCount,
  isConfirmationFlow,
  currency,
  isLoading,
}: {
  overdueCount:          number
  pastDueAmount:         number
  pendingApprovalsCount: number
  isConfirmationFlow:    boolean
  currency:              string
  isLoading:             boolean
}) {
  if (isLoading) return <div className="h-7 w-36 rounded-full animate-pulse bg-[#F4F7F9] dark:bg-white/10" />

  const hasUrgent = overdueCount > 0 || (isConfirmationFlow && pendingApprovalsCount > 0)

  if (!hasUrgent) {
    return (
      <span className="inline-flex items-center gap-1.5">
        <span className="w-1.5 h-1.5 rounded-full bg-green-500" />
        <span className="text-xs font-semibold text-green-600 dark:text-green-400">All caught up</span>
      </span>
    )
  }

  const parts: string[] = []
  if (overdueCount > 0)                            parts.push(`${overdueCount} overdue · ${fmt(pastDueAmount, currency)}`)
  if (isConfirmationFlow && pendingApprovalsCount > 0) parts.push(`${pendingApprovalsCount} pending approval${pendingApprovalsCount > 1 ? 's' : ''}`)

  return (
    <span className="inline-flex items-center gap-1.5">
      <span className="w-1.5 h-1.5 rounded-full bg-red-500" />
      <span className="text-xs font-semibold text-red-600 dark:text-red-400">{parts.join(' · ')}</span>
    </span>
  )
}

// ---------------------------------------------------------------------------
// Status dot for Recent Activity
// ---------------------------------------------------------------------------

const STATUS_META: Record<LifeCycleStatus, { dot: string; text: string; label: string }> = {
  DRAFT:          { dot: 'bg-[#8A9BAE]', text: 'text-c-muted',                            label: 'Draft'    },
  ISSUED:         { dot: 'bg-blue-500',  text: 'text-blue-600 dark:text-blue-400',         label: 'Issued'   },
  PARTIALLY_PAID: { dot: 'bg-[#29B6F6]', text: 'text-[#0D1B2A] dark:text-white',          label: 'Partial'  },
  PAID:           { dot: 'bg-green-500', text: 'text-green-600 dark:text-green-400',       label: 'Paid'     },
  CANCELLED:      { dot: 'bg-red-500',   text: 'text-red-500 dark:text-red-400',           label: 'Cancelled'},
}

// ---------------------------------------------------------------------------
// Recent Activity — compact full-width panel
// ---------------------------------------------------------------------------

function ActivityRow({
  invoice, currency, onClick, isLast,
}: {
  invoice:  InvoiceResponse
  currency: string
  onClick:  () => void
  isLast:   boolean
}) {
  const { dot, text, label } = STATUS_META[invoice.lifeCycleStatus]
  return (
    <li
      className={[
        'flex items-center gap-4 px-5 py-3 cursor-pointer',
        'hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors',
        !isLast ? 'border-b border-c-border' : '',
      ].join(' ')}
      onClick={onClick}
    >
      {/* Invoice # */}
      <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white w-24 shrink-0 tabular-nums">
        {invoice.invoiceNumber}
      </span>

      {/* Status */}
      <span className="inline-flex items-center gap-1.5 shrink-0">
        <span className={`w-1.5 h-1.5 rounded-full ${dot}`} />
        <span className={`text-xs font-medium ${text}`}>{label}</span>
      </span>

      <span className="flex-1" />

      {/* Time ago */}
      <span className="text-xs text-c-muted hidden sm:block tabular-nums">
        {timeAgo(invoice.updatedAt)}
      </span>

      {/* Amount */}
      <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
        {fmt(invoice.totalAmount, currency)}
      </span>

      <ArrowRight size={13} className="text-c-muted shrink-0" />
    </li>
  )
}

function ActivitySkeleton({ isLast }: { isLast: boolean }) {
  return (
    <div className={`flex items-center gap-4 px-5 py-3 animate-pulse ${!isLast ? 'border-b border-c-border' : ''}`}>
      <div className="h-4 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="h-4 w-14 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="flex-1" />
      <div className="h-4 w-10 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="h-4 w-16 rounded bg-[#F4F7F9] dark:bg-white/10" />
    </div>
  )
}

function RecentActivity({
  invoices, currency, isLoading,
}: {
  invoices:  InvoiceResponse[]
  currency:  string
  isLoading: boolean
}) {
  const navigate = useNavigate()
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      <div className="flex items-center justify-between px-5 py-3.5 border-b border-c-border">
        <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Recent Activity</h2>
        <button
          onClick={() => navigate('/invoices')}
          className="text-xs font-medium text-[#29B6F6] hover:underline"
        >
          View all invoices
        </button>
      </div>

      {isLoading ? (
        <>{[...Array(5)].map((_, i) => <ActivitySkeleton key={i} isLast={i === 4} />)}</>
      ) : invoices.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-10 gap-2">
          <p className="text-sm text-c-muted">No invoices yet</p>
          <button
            onClick={() => navigate('/invoices')}
            className="text-sm font-medium text-[#29B6F6] hover:underline"
          >
            Create your first invoice →
          </button>
        </div>
      ) : (
        <ul>
          {invoices.map((inv, idx) => (
            <ActivityRow
              key={inv.id}
              invoice={inv}
              currency={currency}
              isLast={idx === invoices.length - 1}
              onClick={() => navigate(`/invoices/${inv.id}`)}
            />
          ))}
        </ul>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function DashboardPage() {
  const userName = useAuthStore((s) => s.user?.name?.split(' ')[0] ?? 'there')

  const {
    currency,
    isConfirmationFlow,
    isLoading,
    kpis,
    overdueInvoices,
    dueSoonInvoices,
    pendingConfirmations,
    recentInvoices,
  } = useDashboard()

  return (
    <div className="space-y-5">

      {/* ── Header ──────────────────────────────────────────────────────────
          Greeting on the left, health pill on the right.
          The health pill gives an instant status check without scrolling.
      */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">
            {greeting()}, {userName}
          </h1>
          <p className="text-sm text-c-muted mt-0.5">{todayLabel()}</p>
        </div>

        <div className="shrink-0 mt-0.5">
          <HealthPill
            overdueCount={kpis.overdueCount}
            pastDueAmount={kpis.pastDueAmount}
            pendingApprovalsCount={kpis.pendingApprovalsCount}
            isConfirmationFlow={isConfirmationFlow}
            currency={currency}
            isLoading={isLoading}
          />
        </div>
      </div>

      {/* ── KPI Strip ───────────────────────────────────────────────────────
          4 headline numbers: total owed, overdue, collected this month,
          and avg payment delay (or pending approvals for approval-flow orgs).
      */}
      <KpiStrip
        kpis={kpis}
        currency={currency}
        isConfirmationFlow={isConfirmationFlow}
        isLoading={isLoading}
      />

      {/* ── Action panels ───────────────────────────────────────────────────
          Left  — "Needs Attention": overdue invoices ranked by how long
                   they've been outstanding. Approval queue shown below
                   for confirmation-flow orgs.
          Right — "Due Soon": invoices due in the next 14 days, sorted
                   nearest-first with urgency colour coding. Users can act
                   proactively and send reminders before invoices go overdue.
      */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 items-start">
        <ActionTable
          overdueInvoices={overdueInvoices}
          overdueTotal={kpis.overdueCount}
          pendingConfirmations={pendingConfirmations}
          currency={currency}
          isConfirmationFlow={isConfirmationFlow}
          isLoading={isLoading}
        />

        <DueSoonPanel
          invoices={dueSoonInvoices}
          currency={currency}
          isLoading={isLoading}
        />
      </div>

      {/* ── Recent Activity ─────────────────────────────────────────────────
          The 5 most recently touched invoices with status and time ago.
          Gives a quick pulse on what happened without leaving the dashboard.
      */}
      <RecentActivity
        invoices={recentInvoices}
        currency={currency}
        isLoading={isLoading}
      />

    </div>
  )
}
