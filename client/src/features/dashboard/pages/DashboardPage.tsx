import { useState } from 'react'
import { useAuthStore } from '@/store/auth.store'
import { useDashboard } from '../hooks/useDashboard'
import KpiStrip              from '../components/KpiStrip/KpiStrip'
import ActionTable           from '../components/ActionTable/ActionTable'
import CollectionsTrendChart from '../components/CollectionsTrendChart/CollectionsTrendChart'
import AiInsightBanner      from '../components/AiInsightBanner/AiInsightBanner'

import DispatchModal         from '@/features/followups/components/DispatchModal/DispatchModal'
import type { NeedsAttentionItem } from '@/api/dashboard.api'
import { formatCurrency, formatDate } from '@/lib/format'

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
  return formatDate(new Date(), { style: 'long' })
}

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
// AR Aging buckets strip
// ---------------------------------------------------------------------------

type AgingBucket = { label: string; amount: number; count: number }

const AGING_COLORS = [
  { text: 'text-green-600 dark:text-green-400'   },
  { text: 'text-amber-600 dark:text-amber-400'   },
  { text: 'text-orange-600 dark:text-orange-400' },
  { text: 'text-red-600 dark:text-red-400'       },
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
                  {formatCurrency(bucket.amount, currency, { decimals: false })}
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

  const [followupTarget, setFollowupTarget] = useState<NeedsAttentionItem | null>(null)

  const {
    currency,
    isConfirmationFlow,
    isLoading,
    kpis,
    agingBuckets,
    collectionsTrend,
    needsAttention,
  } = useDashboard()

  return (
    <>
    <div className="space-y-5">

      {/* ── Header ──────────────────────────────────────────────────────────── */}
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

      {/* ── KPI Strip ───────────────────────────────────────────────────────── */}
      <KpiStrip
        kpis={kpis}
        currency={currency}
        isConfirmationFlow={isConfirmationFlow}
        isLoading={isLoading}
      />

      {/* ── Needs Attention + AI Insights ───────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-4 items-start">
        <div className="lg:col-span-3">
          <ActionTable
            invoices={needsAttention}
            currency={currency}
            isLoading={isLoading}
            onFollowup={setFollowupTarget}
          />
        </div>
        <div className="lg:col-span-2">
          <AiInsightBanner />
        </div>
      </div>

      {/* ── AR Aging ────────────────────────────────────────────────────────── */}
      <AgingBuckets
        buckets={agingBuckets}
        currency={currency}
        isLoading={isLoading}
      />

      {/* ── Collections Trend ───────────────────────────────────────────────── */}
      <CollectionsTrendChart
        data={collectionsTrend}
        currency={currency}
        isLoading={isLoading}
      />

    </div>

    {/* ── Quick-action modals ──────────────────────────────────────────────── */}
    {followupTarget && (
      <DispatchModal
        invoice={{ id: followupTarget.invoiceId, invoiceNumber: followupTarget.invoiceNumber, customerId: followupTarget.customerId } as any}
        onClose={() => setFollowupTarget(null)}
      />
    )}
    </>
  )
}
