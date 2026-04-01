import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Zap } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { useDashboard } from '../hooks/useDashboard'
import KpiStrip              from '../components/KpiStrip/KpiStrip'
import ActionTable           from '../components/ActionTable/ActionTable'
import CollectionsTrendChart from '../components/CollectionsTrendChart/CollectionsTrendChart'
import AiInsightBanner      from '../components/AiInsightBanner/AiInsightBanner'
import { useToggleAutoRecovery } from '@/features/recover/hooks/useRecover'

import DispatchModal         from '@/features/followups/components/DispatchModal/DispatchModal'
import type { NeedsAttentionItem } from '@/api/dashboard.api'
import { formatCurrency, formatDate } from '@/lib/format'
import { useRecoverStats } from '@/features/recover/hooks/useRecover'

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
// Automation agent status banner
// ---------------------------------------------------------------------------

function AutoAgentBanner() {
  const enabled = useAuthStore((s) => s.org?.autoRecoveryEnabled ?? false)
  const { data, isLoading } = useRecoverStats()

  if (!enabled) return null
  if (isLoading || !data) return null

  const pills: { label: string; value: string }[] = [
    { label: 'scheduled today', value: String(data.pendingToday) },
    { label: 'sent this week',  value: String(data.sentThisWeek) },
    { label: 'sent today',      value: String(data.sentToday) },
  ]

  const h = new Date().getHours()
  const tagline =
    h < 6  ? 'Up late? The engine never sleeps.' :
    h < 12 ? 'Grab a coffee — the engine is on it.' :
    h < 17 ? 'Take a break, the engine\'s handling it.' :
             'Wind down — the engine keeps working.'

  return (
    <div className="flex items-center gap-3 px-3.5 py-2.5 rounded-xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/25 text-sm">
      <Zap size={14} className="shrink-0 text-amber-500 animate-pulse" strokeWidth={2.5} />
      <span className="text-amber-700 dark:text-amber-400 font-medium text-xs">Automation engine</span>
      <span className="text-amber-300 dark:text-amber-600 select-none">·</span>
      <div className="flex items-center gap-3 flex-wrap">
        {pills.map((p, i) => (
          <span key={i} className="text-xs text-amber-700 dark:text-amber-400">
            <span className="font-bold">{p.value}</span>
            <span className="opacity-70 ml-1">{p.label}</span>
          </span>
        ))}
        <span className="text-amber-300 dark:text-amber-600 select-none hidden sm:inline">·</span>
        <span className="text-xs text-amber-600/70 dark:text-amber-500/70 italic hidden sm:inline">{tagline}</span>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Auto Recovery widget — header shortcut to Recover page
// ---------------------------------------------------------------------------

function AutoRecoveryWidget() {
  const enabled    = useAuthStore((s) => s.org?.autoRecoveryEnabled ?? false)
  const toggleMut  = useToggleAutoRecovery()

  return (
    <div className="flex items-center gap-2">
      {/* Status pill */}
      <Link
        to="/recover"
        className="flex items-center gap-1.5 px-2.5 py-1 rounded-full border transition-colors
          border-amber-300 dark:border-amber-500/40
          bg-amber-50 dark:bg-amber-500/10
          hover:bg-amber-100 dark:hover:bg-amber-500/20"
        title="Go to Recover page"
      >
        {enabled && (
          <span className="relative flex h-2 w-2 shrink-0">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500" />
          </span>
        )}
        <Zap size={11} className={enabled ? 'text-amber-600 dark:text-amber-400' : 'text-c-muted'} strokeWidth={2.5} />
        <span className={`text-[11px] font-semibold ${enabled ? 'text-amber-700 dark:text-amber-400' : 'text-c-muted'}`}>
          {enabled ? 'AUTO ON' : 'AUTO OFF'}
        </span>
      </Link>

      {/* Toggle */}
      <button
        onClick={() => toggleMut.mutate(!enabled)}
        disabled={toggleMut.isPending}
        aria-label={enabled ? 'Disable auto recovery' : 'Enable auto recovery'}
        className={[
          'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent',
          'transition-colors duration-200 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed',
          enabled ? 'bg-amber-400' : 'bg-[#8A9BAE]/30',
        ].join(' ')}
      >
        <span className={[
          'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow transition duration-200',
          enabled ? 'translate-x-4' : 'translate-x-0',
        ].join(' ')} />
      </button>
    </div>
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
  const userName            = useAuthStore((s) => s.user?.name?.split(' ')[0] ?? 'there')
  const autoRecoveryEnabled = useAuthStore((s) => s.org?.autoRecoveryEnabled ?? false)

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
          <AutoRecoveryWidget />
        </div>
      </div>

      {/* ── Automation agent banner ─────────────────────────────────────────── */}
      <AutoAgentBanner />

      {/* ── KPI Strip ───────────────────────────────────────────────────────── */}
      <KpiStrip
        kpis={kpis}
        currency={currency}
        isConfirmationFlow={isConfirmationFlow}
        isLoading={isLoading}
        autoRecoveryEnabled={autoRecoveryEnabled}
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
