import { memo } from 'react'
import { AlertTriangle } from 'lucide-react'
import type { PaymentConfirmationResponse, ConfirmationStatus } from '@/types/confirmation.types'
import { formatCurrency, formatDate } from '@/lib/format'
import { timeAgoFine as timeAgo } from '@/utils/date'

// ---------------------------------------------------------------------------
// Claim type
// ---------------------------------------------------------------------------

type ClaimType = 'FULL' | 'PARTIAL' | 'OVER'

function getClaimType(c: PaymentConfirmationResponse): ClaimType {
  if (c.amountClaimed > c.invoiceRemainingAmount) return 'OVER'
  if (c.amountClaimed < c.invoiceRemainingAmount) return 'PARTIAL'
  return 'FULL'
}

// ---------------------------------------------------------------------------
// Status — dot + text, no background shading
// ---------------------------------------------------------------------------

const STATUS_META: Record<ConfirmationStatus, { dot: string; text: string; label: string }> = {
  PENDING_APPROVAL:    { dot: 'bg-amber-400',  text: 'text-amber-600 dark:text-amber-400',       label: 'Pending'             },
  APPROVED:            { dot: 'bg-green-500',  text: 'text-green-600 dark:text-green-400',        label: 'Approved'            },
  REMAINING_REQUESTED: { dot: 'bg-blue-500',   text: 'text-blue-600 dark:text-blue-400',          label: 'Remaining Requested' },
  REJECTED:            { dot: 'bg-red-500',    text: 'text-red-500 dark:text-red-400',            label: 'Rejected'            },
}

// ---------------------------------------------------------------------------
// ConfirmationCard
// ---------------------------------------------------------------------------

interface Props {
  confirmation:       PaymentConfirmationResponse
  currency:           string
  onApprove:          () => void
  onRequestRemaining: () => void
  onReject:           () => void
  isActing:           boolean
}

const ConfirmationCard = memo(function ConfirmationCard({
  confirmation: c,
  currency,
  onApprove,
  onRequestRemaining,
  onReject,
  isActing,
}: Props) {
  const claimType = getClaimType(c)
  const isPending = c.status === 'PENDING_APPROVAL'
  const { dot, text, label } = STATUS_META[c.status]

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-4 sm:p-5">

      {/* ── Top row: invoice # · status · time ── */}
      <div className="flex items-center gap-3 flex-wrap">
        <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white">
          {c.invoiceNumber}
        </span>

        <span className={`text-xs font-medium ${text}`}>{label}</span>

        <span className="ml-auto text-xs text-c-muted shrink-0">
          {timeAgo(c.createdAt)}
        </span>
      </div>

      {/* ── Claimed amount + context ── */}
      <div className="mt-3 flex items-baseline gap-2 flex-wrap">
        <span className="text-xl font-bold text-[#0D1B2A] dark:text-white tabular-nums">
          {formatCurrency(c.amountClaimed, currency, { decimals: false })}
        </span>
        <span className="text-xs text-c-muted">
          claimed
          {claimType === 'FULL'    && ' · full payment'}
          {claimType === 'PARTIAL' && ` · ${formatCurrency(c.invoiceRemainingAmount - c.amountClaimed, currency, { decimals: false })} still remaining`}
          {claimType === 'OVER'    && ' · exceeds balance'}
        </span>
      </div>

      {/* ── Overpayment warning ── */}
      {claimType === 'OVER' && isPending && (
        <div className="mt-3 flex items-start gap-2 p-3 rounded-lg bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
          <AlertTriangle size={14} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
          <p className="text-xs text-amber-700 dark:text-amber-400">
            Claimed amount exceeds the remaining balance. Verify with the customer before approving.
          </p>
        </div>
      )}

      {/* ── Customer note ── */}
      {c.customerNote && (
        <div className="mt-3 p-3 rounded-lg bg-[#F4F7F9] dark:bg-[#243447]">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-c-muted mb-1">Customer Note</p>
          <p className="text-xs text-[#0D1B2A] dark:text-white leading-relaxed">{c.customerNote}</p>
        </div>
      )}

      {/* ── Business note (post-review) ── */}
      {c.businessNote && !isPending && (
        <div className="mt-3 p-3 rounded-lg border border-c-border">
          <div className="flex items-center gap-2 mb-1">
            <p className="text-[10px] font-semibold uppercase tracking-wide text-c-muted">Your Note</p>
            <p className="text-[10px] text-c-muted">{formatDate(c.reviewedAt)}</p>
          </div>
          <p className="text-xs text-[#0D1B2A] dark:text-white leading-relaxed">{c.businessNote}</p>
        </div>
      )}

      {/* ── Action buttons ── */}
      {isPending && (
        <div className="mt-4 flex gap-2 flex-wrap">
          {/* Full / overpayment: Approve + Reject */}
          {(claimType === 'FULL' || claimType === 'OVER') && (
            <>
              <button
                onClick={onApprove}
                disabled={isActing}
                className="flex-1 min-w-[90px] py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 hover:opacity-90 transition-opacity"
                style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
              >
                {isActing ? '…' : 'Approve'}
              </button>
              <button
                onClick={onReject}
                disabled={isActing}
                className="flex-1 min-w-[90px] py-2 rounded-lg text-sm font-semibold text-red-500 border border-red-200 dark:border-red-500/30 hover:bg-red-50 dark:hover:bg-red-500/10 disabled:opacity-50 transition-colors"
              >
                Reject
              </button>
            </>
          )}

          {/* Partial: Request Remaining → Approve as Final → Reject */}
          {claimType === 'PARTIAL' && (
            <>
              <button
                onClick={onRequestRemaining}
                disabled={isActing}
                className="flex-1 min-w-[110px] py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 hover:opacity-90 transition-opacity"
                style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
              >
                Request Remaining
              </button>
              <button
                onClick={onApprove}
                disabled={isActing}
                className="flex-1 min-w-[110px] py-2 rounded-lg text-sm font-semibold text-[#29B6F6] border border-[#29B6F6]/40 hover:bg-[#29B6F6]/10 disabled:opacity-50 transition-colors"
              >
                {isActing ? '…' : 'Approve as Final'}
              </button>
              <button
                onClick={onReject}
                disabled={isActing}
                className="w-full py-2 rounded-lg text-sm font-semibold text-red-500 border border-red-200 dark:border-red-500/30 hover:bg-red-50 dark:hover:bg-red-500/10 disabled:opacity-50 transition-colors"
              >
                Reject
              </button>
            </>
          )}
        </div>
      )}
    </div>
  )
})

export default ConfirmationCard
