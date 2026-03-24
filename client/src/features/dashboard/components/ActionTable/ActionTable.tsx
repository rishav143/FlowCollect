import { useNavigate } from 'react-router-dom'
import { ArrowRight, CheckCircle2, Clock } from 'lucide-react'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { PaymentConfirmationResponse } from '@/types/confirmation.types'

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

function daysOverdue(dueDate: string): number {
  const due   = new Date(dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.max(0, Math.floor((today.getTime() - due.getTime()) / 86_400_000))
}

function timeAgo(iso: string): string {
  const mins = Math.floor((Date.now() - new Date(iso).getTime()) / 60_000)
  if (mins < 60)  return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24)   return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

// ---------------------------------------------------------------------------
// Section wrapper
// ---------------------------------------------------------------------------

function Section({
  title,
  icon,
  count,
  iconColor,
  children,
}: {
  title:     string
  icon:      React.ReactNode
  count:     number
  iconColor: string
  children:  React.ReactNode
}) {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      <div className="flex items-center gap-2 px-5 py-4 border-b border-c-border">
        <span className={iconColor}>{icon}</span>
        <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{title}</h2>
        {count > 0 && (
          <span className="ml-auto text-xs font-medium text-c-muted">{count} item{count !== 1 ? 's' : ''}</span>
        )}
      </div>
      {children}
    </div>
  )
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex items-center justify-center py-10">
      <p className="text-sm text-c-muted">{message}</p>
    </div>
  )
}

function RowSkeleton() {
  return (
    <div className="flex items-center gap-3 px-5 py-3.5 animate-pulse">
      <div className="h-4 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="flex-1 h-4 w-24 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="h-4 w-16 rounded bg-[#F4F7F9] dark:bg-white/10" />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Overdue invoices table
// ---------------------------------------------------------------------------

function OverdueSection({
  invoices,
  overdueTotal,
  currency,
  isLoading,
}: {
  invoices:     InvoiceResponse[]
  overdueTotal: number
  currency:     string
  isLoading:    boolean
}) {
  const navigate = useNavigate()

  return (
    <Section
      title="Needs Attention"
      icon={<Clock size={15} strokeWidth={2} />}
      count={invoices.length}
      iconColor="text-red-500"
    >
      {isLoading ? (
        <>{[...Array(3)].map((_, i) => <RowSkeleton key={i} />)}</>
      ) : invoices.length === 0 ? (
        <EmptyState message="No overdue invoices — you're all caught up!" />
      ) : (
        <>
          <ul>
            {invoices.map((inv, idx) => {
              const days = inv.dueDate ? daysOverdue(inv.dueDate) : 0
              return (
                <li
                  key={inv.id}
                  className={[
                    'flex items-center gap-3 px-5 py-3.5 cursor-pointer',
                    'hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors',
                    idx < invoices.length - 1 ? 'border-b border-c-border' : '',
                  ].join(' ')}
                  onClick={() => navigate(`/invoices/${inv.id}`)}
                >
                  <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white w-24 shrink-0">
                    {inv.invoiceNumber}
                  </span>

                  <span className="inline-flex items-center gap-1.5 shrink-0">
                    <span className="w-1.5 h-1.5 rounded-full bg-red-500" />
                    <span className="text-xs font-medium text-red-600 dark:text-red-400">{days}d overdue</span>
                  </span>

                  <span className="flex-1" />

                  <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
                    {fmt(inv.remainingAmount, currency)}
                  </span>

                  <ArrowRight size={14} className="text-c-muted shrink-0" />
                </li>
              )
            })}
          </ul>
          {overdueTotal > invoices.length && (
            <div className="border-t border-c-border px-5 py-3">
              <button
                onClick={() => navigate('/invoices')}
                className="text-xs font-medium text-[#29B6F6] hover:underline"
              >
                View all {overdueTotal} overdue invoices →
              </button>
            </div>
          )}
        </>
      )}
    </Section>
  )
}

// ---------------------------------------------------------------------------
// Pending approvals table
// ---------------------------------------------------------------------------

function ApprovalsSection({
  confirmations,
  currency,
  isLoading,
}: {
  confirmations: PaymentConfirmationResponse[]
  currency:      string
  isLoading:     boolean
}) {
  const navigate = useNavigate()

  return (
    <Section
      title="Pending Approvals"
      icon={<CheckCircle2 size={15} strokeWidth={2} />}
      count={confirmations.length}
      iconColor="text-amber-500"
    >
      {isLoading ? (
        <>{[...Array(3)].map((_, i) => <RowSkeleton key={i} />)}</>
      ) : confirmations.length === 0 ? (
        <EmptyState message="No pending payment claims" />
      ) : (
        <ul>
          {confirmations.map((conf, idx) => (
            <li
              key={conf.id}
              className={[
                'flex items-center gap-3 px-5 py-3.5 cursor-pointer',
                'hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors',
                idx < confirmations.length - 1 ? 'border-b border-c-border' : '',
              ].join(' ')}
              onClick={() => navigate('/approvals')}
            >
              {/* Invoice number */}
              <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white w-24 shrink-0">
                {conf.invoiceNumber}
              </span>

              {/* Time ago */}
              <span className="text-xs text-c-muted">{timeAgo(conf.createdAt)}</span>

              <span className="flex-1" />

              {/* Amount claimed */}
              <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
                {fmt(conf.amountClaimed, currency)}
              </span>

              <ArrowRight size={14} className="text-c-muted shrink-0" />
            </li>
          ))}
        </ul>
      )}
    </Section>
  )
}

// ---------------------------------------------------------------------------
// Exported component
// ---------------------------------------------------------------------------

interface Props {
  overdueInvoices:      InvoiceResponse[]
  overdueTotal:         number
  pendingConfirmations: PaymentConfirmationResponse[]
  currency:             string
  isConfirmationFlow:   boolean
  isLoading:            boolean
}

export default function ActionTable({
  overdueInvoices,
  overdueTotal,
  pendingConfirmations,
  currency,
  isConfirmationFlow,
  isLoading,
}: Props) {
  return (
    <div className="space-y-4">
      <OverdueSection
        invoices={overdueInvoices}
        overdueTotal={overdueTotal}
        currency={currency}
        isLoading={isLoading}
      />
      {isConfirmationFlow && (
        <ApprovalsSection
          confirmations={pendingConfirmations}
          currency={currency}
          isLoading={isLoading}
        />
      )}
    </div>
  )
}
