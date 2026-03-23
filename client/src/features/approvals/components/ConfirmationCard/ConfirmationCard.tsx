import { AlertTriangle, CheckCircle, XCircle, Clock, RefreshCw } from 'lucide-react'
import type { PaymentConfirmationResponse, ConfirmationStatus } from '@/types/confirmation.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

function fmtDateTime(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

// ---------------------------------------------------------------------------
// Claim type logic
// ---------------------------------------------------------------------------

type ClaimType = 'FULL' | 'PARTIAL' | 'OVER'

function getClaimType(c: PaymentConfirmationResponse): ClaimType {
  if (c.amountClaimed > c.invoiceRemainingAmount) return 'OVER'
  if (c.amountClaimed < c.invoiceRemainingAmount) return 'PARTIAL'
  return 'FULL'
}

// ---------------------------------------------------------------------------
// Status badge
// ---------------------------------------------------------------------------

const STATUS_META: Record<ConfirmationStatus, { label: string; icon: React.ReactNode; chip: string }> = {
  PENDING_APPROVAL: {
    label: 'Pending',
    icon: <Clock size={12} />,
    chip: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400',
  },
  APPROVED: {
    label: 'Approved',
    icon: <CheckCircle size={12} />,
    chip: 'bg-green-50 text-green-600 dark:bg-green-500/15 dark:text-green-400',
  },
  REMAINING_REQUESTED: {
    label: 'Remaining Requested',
    icon: <RefreshCw size={12} />,
    chip: 'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400',
  },
  REJECTED: {
    label: 'Rejected',
    icon: <XCircle size={12} />,
    chip: 'bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400',
  },
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

interface Props {
  confirmation: PaymentConfirmationResponse
  currency:     string
  onApprove:          () => void
  onRequestRemaining: () => void
  onReject:           () => void
  isActing:     boolean
}

export default function ConfirmationCard({
  confirmation: c,
  currency,
  onApprove,
  onRequestRemaining,
  onReject,
  isActing,
}: Props) {
  const claimType = getClaimType(c)
  const isPending = c.status === 'PENDING_APPROVAL'
  const meta      = STATUS_META[c.status]

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 p-5 space-y-4">

      {/* Header row */}
      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">
            {c.invoiceNumber}
          </p>
          <p className="text-xs text-[#8A9BAE] mt-0.5">
            Submitted {fmtDateTime(c.createdAt)}
          </p>
        </div>
        <span className={`inline-flex items-center gap-1 text-[10px] font-semibold px-2.5 py-1 rounded-full ${meta.chip}`}>
          {meta.icon}
          {meta.label}
        </span>
      </div>

      {/* Overpayment warning */}
      {claimType === 'OVER' && isPending && (
        <div className="flex items-start gap-2.5 p-3 rounded-lg bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
          <AlertTriangle size={15} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
          <p className="text-xs text-amber-700 dark:text-amber-400">
            Amount claimed (<span className="font-semibold">{fmt(c.amountClaimed, currency)}</span>) exceeds the remaining balance (<span className="font-semibold">{fmt(c.invoiceRemainingAmount, currency)}</span>). Verify before approving.
          </p>
        </div>
      )}

      {/* Amount breakdown */}
      <div className="grid grid-cols-3 gap-3">
        <div className="bg-[#F4F7F9] dark:bg-[#243447] rounded-lg p-3 text-center">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-[#8A9BAE] mb-1">Claimed</p>
          <p className={`text-sm font-bold tabular-nums ${
            claimType === 'OVER'
              ? 'text-amber-600 dark:text-amber-400'
              : 'text-[#0D1B2A] dark:text-white'
          }`}>
            {fmt(c.amountClaimed, currency)}
          </p>
        </div>
        <div className="bg-[#F4F7F9] dark:bg-[#243447] rounded-lg p-3 text-center">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-[#8A9BAE] mb-1">Remaining</p>
          <p className="text-sm font-bold tabular-nums text-red-500">{fmt(c.invoiceRemainingAmount, currency)}</p>
        </div>
        <div className="bg-[#F4F7F9] dark:bg-[#243447] rounded-lg p-3 text-center">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-[#8A9BAE] mb-1">Total</p>
          <p className="text-sm font-bold tabular-nums text-[#0D1B2A] dark:text-white">{fmt(c.invoiceTotalAmount, currency)}</p>
        </div>
      </div>

      {/* Customer note */}
      {c.customerNote && (
        <div className="rounded-lg bg-[#F4F7F9] dark:bg-[#243447] p-3">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-[#8A9BAE] mb-1">Customer Note</p>
          <p className="text-xs text-[#0D1B2A] dark:text-white leading-relaxed">{c.customerNote}</p>
        </div>
      )}

      {/* Business note (post-review) */}
      {c.businessNote && !isPending && (
        <div className="rounded-lg border border-[#F4F7F9] dark:border-white/10 p-3">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-[#8A9BAE] mb-1">Your Note</p>
          <p className="text-xs text-[#0D1B2A] dark:text-white leading-relaxed">{c.businessNote}</p>
          {c.reviewedAt && (
            <p className="text-[10px] text-[#8A9BAE] mt-1.5">Reviewed {fmtDateTime(c.reviewedAt)}</p>
          )}
        </div>
      )}

      {/* Action buttons — only for PENDING_APPROVAL */}
      {isPending && (
        <div className="flex flex-wrap gap-2 pt-1">
          {/* Full-payment or overpayment: Approve + Reject */}
          {(claimType === 'FULL' || claimType === 'OVER') && (
            <>
              <button
                onClick={onApprove}
                disabled={isActing}
                className="flex-1 min-w-[100px] py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 transition-opacity hover:opacity-90"
                style={{ background: 'linear-gradient(90deg, #2E7A8E 0%, #29B6F6 100%)' }}
              >
                {isActing ? '…' : 'Approve'}
              </button>
              <button
                onClick={onReject}
                disabled={isActing}
                className="flex-1 min-w-[100px] py-2 rounded-lg text-sm font-semibold text-red-500 border border-red-200 dark:border-red-500/30 hover:bg-red-50 dark:hover:bg-red-500/10 disabled:opacity-50 transition-colors"
              >
                Reject
              </button>
            </>
          )}

          {/* Partial: Approve Partial + Request Remaining + Reject */}
          {claimType === 'PARTIAL' && (
            <>
              <button
                onClick={onApprove}
                disabled={isActing}
                className="flex-1 min-w-[120px] py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 transition-opacity hover:opacity-90"
                style={{ background: 'linear-gradient(90deg, #2E7A8E 0%, #29B6F6 100%)' }}
              >
                {isActing ? '…' : 'Approve Partial'}
              </button>
              <button
                onClick={onRequestRemaining}
                disabled={isActing}
                className="flex-1 min-w-[120px] py-2 rounded-lg text-sm font-semibold text-[#29B6F6] border border-[#29B6F6]/40 hover:bg-[#29B6F6]/10 disabled:opacity-50 transition-colors"
              >
                Request Remaining
              </button>
              <button
                onClick={onReject}
                disabled={isActing}
                className="w-full py-2 rounded-lg text-sm font-medium text-[#8A9BAE] hover:bg-[#F4F7F9] dark:hover:bg-[#243447] disabled:opacity-50 transition-colors"
              >
                Reject
              </button>
            </>
          )}
        </div>
      )}
    </div>
  )
}
