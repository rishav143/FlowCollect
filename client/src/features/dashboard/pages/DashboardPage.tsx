import { useNavigate } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { useDashboard } from '../hooks/useDashboard'
import KpiStrip   from '../components/KpiStrip/KpiStrip'
import ActionTable from '../components/ActionTable/ActionTable'
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
    weekday: 'long',
    day:     'numeric',
    month:   'long',
    year:    'numeric',
  })
}

function fmt(amount: number, currency: string) {
  return new Intl.NumberFormat('en-IN', {
    style:                 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(amount)
}

// ---------------------------------------------------------------------------
// Status chip
// ---------------------------------------------------------------------------

const STATUS_STYLES: Record<LifeCycleStatus, string> = {
  DRAFT:           'bg-[#8A9BAE]/10 text-[#8A9BAE]',
  ISSUED:          'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400',
  PARTIALLY_PAID:  'bg-[#29B6F6]/10 text-[#29B6F6]',
  PAID:            'bg-green-50 text-green-600 dark:bg-green-500/15 dark:text-green-400',
  CANCELLED:       'bg-red-50 text-red-500 dark:bg-red-500/10 dark:text-red-400',
}

function StatusChip({ status }: { status: LifeCycleStatus }) {
  return (
    <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${STATUS_STYLES[status]}`}>
      {status.replace('_', ' ')}
    </span>
  )
}

// ---------------------------------------------------------------------------
// Recent invoices table
// ---------------------------------------------------------------------------

function RecentInvoiceRow({
  invoice,
  currency,
  onClick,
  isLast,
}: {
  invoice:  InvoiceResponse
  currency: string
  onClick:  () => void
  isLast:   boolean
}) {
  return (
    <li
      className={[
        'flex items-center gap-4 px-5 py-3.5 cursor-pointer',
        'hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors',
        !isLast ? 'border-b border-[#F4F7F9] dark:border-white/10' : '',
      ].join(' ')}
      onClick={onClick}
    >
      <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white w-24 shrink-0">
        {invoice.invoiceNumber}
      </span>

      <StatusChip status={invoice.lifeCycleStatus} />

      <span className="flex-1" />

      {invoice.dueDate && (
        <span className="text-xs text-[#8A9BAE] hidden sm:block tabular-nums">
          Due {new Date(invoice.dueDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
        </span>
      )}

      <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
        {fmt(invoice.totalAmount, currency)}
      </span>

      <ArrowRight size={14} className="text-[#8A9BAE] shrink-0" />
    </li>
  )
}

function RecentInvoicesRowSkeleton() {
  return (
    <div className="flex items-center gap-4 px-5 py-3.5 animate-pulse border-b border-[#F4F7F9] dark:border-white/10">
      <div className="h-4 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="h-5 w-16 rounded-full bg-[#F4F7F9] dark:bg-white/10" />
      <div className="flex-1" />
      <div className="h-4 w-16 rounded bg-[#F4F7F9] dark:bg-white/10" />
    </div>
  )
}

function RecentInvoices({
  invoices,
  currency,
  isLoading,
}: {
  invoices:  InvoiceResponse[]
  currency:  string
  isLoading: boolean
}) {
  const navigate = useNavigate()

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 overflow-hidden">
      <div className="flex items-center justify-between px-5 py-4 border-b border-[#F4F7F9] dark:border-white/10">
        <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Recent Invoices</h2>
        <button
          onClick={() => navigate('/invoices')}
          className="text-xs text-[#29B6F6] hover:underline font-medium"
        >
          View all
        </button>
      </div>

      {isLoading ? (
        <>{[...Array(5)].map((_, i) => <RecentInvoicesRowSkeleton key={i} />)}</>
      ) : invoices.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 gap-2">
          <p className="text-sm text-[#8A9BAE]">No invoices yet</p>
          <button
            onClick={() => navigate('/invoices')}
            className="text-sm text-[#29B6F6] hover:underline font-medium"
          >
            Create your first invoice →
          </button>
        </div>
      ) : (
        <ul>
          {invoices.map((inv, idx) => (
            <RecentInvoiceRow
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
    pendingConfirmations,
    recentInvoices,
  } = useDashboard()

  return (
    <div className="space-y-6">

      {/* ── Header ──────────────────────────────────────────────────────── */}
      <div>
        <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">
          {greeting()}, {userName}
        </h1>
        <p className="text-sm text-[#8A9BAE] mt-0.5">{todayLabel()}</p>
      </div>

      {/* ── KPI Strip ───────────────────────────────────────────────────── */}
      <KpiStrip
        kpis={kpis}
        currency={currency}
        isConfirmationFlow={isConfirmationFlow}
        isLoading={isLoading}
      />

      {/* ── Action items ────────────────────────────────────────────────── */}
      <ActionTable
        overdueInvoices={overdueInvoices}
        pendingConfirmations={pendingConfirmations}
        currency={currency}
        isConfirmationFlow={isConfirmationFlow}
        isLoading={isLoading}
      />

      {/* ── Recent invoices ─────────────────────────────────────────────── */}
      <RecentInvoices
        invoices={recentInvoices}
        currency={currency}
        isLoading={isLoading}
      />

    </div>
  )
}
