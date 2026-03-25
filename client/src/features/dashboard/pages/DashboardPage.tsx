import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { useDashboard } from '../hooks/useDashboard'
import KpiStrip     from '../components/KpiStrip/KpiStrip'
import ActionTable  from '../components/ActionTable/ActionTable'
import DueSoonPanel from '../components/DueSoonPanel/DueSoonPanel'
import RecordPaymentModal from '@/features/invoices/modals/RecordPaymentModal'
import FollowupModal      from '@/features/invoices/modals/FollowupModal'
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

import { timeAgoFine as timeAgo } from '@/utils/date'

// ---------------------------------------------------------------------------
// Health pill — at-a-glance status shown in the header
// ---------------------------------------------------------------------------

function HealthPill({
  overdueCount,
  pendingApprovalsCount,
  isConfirmationFlow,
  isLoading,
}: {
  overdueCount:          number
  pendingApprovalsCount: number
  isConfirmationFlow:    boolean
  isLoading:             boolean
}) {
  if (isLoading) return <div className="h-5 w-24 rounded-full animate-pulse bg-[#F4F7F9] dark:bg-white/10" />

  const hasUrgent = overdueCount > 0 || (isConfirmationFlow && pendingApprovalsCount > 0)

  if (!hasUrgent) {
    return (
      <span className="text-xs font-medium text-green-600 dark:text-green-400">All caught up</span>
    )
  }

  const parts: string[] = []
  if (overdueCount > 0)
    parts.push(`${overdueCount} overdue`)
  if (isConfirmationFlow && pendingApprovalsCount > 0)
    parts.push(`${pendingApprovalsCount} pending`)

  return (
    <span className="text-xs font-medium text-red-600 dark:text-red-400">{parts.join(' · ')}</span>
  )
}

// ---------------------------------------------------------------------------
// Status dot for Recent Activity
// ---------------------------------------------------------------------------

const STATUS_META: Record<LifeCycleStatus, { text: string; label: string }> = {
  DRAFT:          { text: 'text-c-muted',                            label: 'Draft'    },
  ISSUED:         { text: 'text-blue-600 dark:text-blue-400',        label: 'Issued'   },
  PARTIALLY_PAID: { text: 'text-[#0D1B2A] dark:text-white',         label: 'Partial'  },
  PAID:           { text: 'text-green-600 dark:text-green-400',      label: 'Paid'     },
  CANCELLED:      { text: 'text-red-500 dark:text-red-400',          label: 'Cancelled'},
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
  const { text, label } = STATUS_META[invoice.lifeCycleStatus]
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
      <span className={`text-xs font-medium shrink-0 ${text}`}>{label}</span>

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
// AR Aging buckets strip
// ---------------------------------------------------------------------------

type AgingBucket = { label: string; amount: number; count: number }

const AGING_COLORS = [
  { text: 'text-green-600 dark:text-green-400'  },
  { text: 'text-amber-600 dark:text-amber-400'  },
  { text: 'text-orange-600 dark:text-orange-400' },
  { text: 'text-red-600 dark:text-red-400'      },
]

function AgingBuckets({
  buckets,
  currency,
  isLoading,
}: {
  buckets:   { current: AgingBucket; d1_30: AgingBucket; d31_60: AgingBucket; d60plus: AgingBucket }
  currency:  string
  isLoading: boolean
}) {
  const items = [buckets.current, buckets.d1_30, buckets.d31_60, buckets.d60plus]
  const hasAny = items.some((b) => b.count > 0)

  if (!isLoading && !hasAny) return null

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border px-5 py-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-c-muted mb-3">
        Receivables Aging
      </p>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {items.map((bucket, i) => (
          <div key={bucket.label} className={isLoading ? 'animate-pulse' : ''}>
            {isLoading ? (
              <div className="space-y-1.5">
                <div className="h-3 w-12 rounded bg-[#F4F7F9] dark:bg-white/10" />
                <div className="h-5 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
              </div>
            ) : (
              <>
                <div className="mb-0.5">
                  <span className={`text-xs font-medium ${AGING_COLORS[i].text}`}>{bucket.label}</span>
                </div>
                <p className="text-base font-bold text-[#0D1B2A] dark:text-white tabular-nums">
                  {new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(bucket.amount)}
                </p>
                <p className="text-xs text-c-muted">{bucket.count} invoice{bucket.count !== 1 ? 's' : ''}</p>
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function DashboardPage() {
  const userName = useAuthStore((s) => s.user?.name?.split(' ')[0] ?? 'there')

  const [payTarget,      setPayTarget]      = useState<InvoiceResponse | null>(null)
  const [followupTarget, setFollowupTarget] = useState<InvoiceResponse | null>(null)

  const {
    currency,
    isConfirmationFlow,
    isLoading,
    kpis,
    agingBuckets,
    overdueInvoices,
    dueSoonInvoices,
    pendingConfirmations,
    recentInvoices,
  } = useDashboard()

  return (
    <>
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
            pendingApprovalsCount={kpis.pendingApprovalsCount}
            isConfirmationFlow={isConfirmationFlow}
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

      {/* ── AR Aging ────────────────────────────────────────────────────────
          Breaks unpaid receivables into 4 age buckets so the user can see
          at a glance how long money has been sitting outstanding.
      */}
      <AgingBuckets
        buckets={agingBuckets}
        currency={currency}
        isLoading={isLoading}
      />

      {/* ── Action panels ───────────────────────────────────────────────────
          Left  — "Needs Attention": overdue invoices ranked by how long
                   they've been outstanding. Quick Pay / Remind buttons on
                   hover so the user can act without leaving the dashboard.
          Right — "Due Soon": invoices due in the next 14 days, sorted
                   nearest-first. Quick Remind on hover for proactive outreach.
      */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 items-start">
        <ActionTable
          overdueInvoices={overdueInvoices}
          overdueTotal={kpis.overdueCount}
          pendingConfirmations={pendingConfirmations}
          currency={currency}
          isConfirmationFlow={isConfirmationFlow}
          isLoading={isLoading}
          onPay={setPayTarget}
          onFollowup={setFollowupTarget}
        />

        <DueSoonPanel
          invoices={dueSoonInvoices}
          currency={currency}
          isLoading={isLoading}
          onFollowup={setFollowupTarget}
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

    {/* ── Quick-action modals ─────────────────────────────────────────────── */}
    {payTarget && (
      <RecordPaymentModal
        invoiceId={payTarget.id}
        remainingAmount={payTarget.remainingAmount}
        currency={currency}
        onClose={() => setPayTarget(null)}
      />
    )}
    {followupTarget && (
      <FollowupModal
        invoiceNumber={followupTarget.invoiceNumber}
        onClose={() => setFollowupTarget(null)}
      />
    )}
  </>
  )
}
